package com.thesystem.modules.task.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.provider.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ExecutionStateValidationTest {

    private TaskExecutionProviderRegistry registry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        registry = new TaskExecutionProviderRegistry(
                new BooleanExecutionProvider(),
                new ChecklistExecutionProvider(),
                new TimerExecutionProvider(),
                new CountExecutionProvider(),
                new ProgressExecutionProvider(),
                new HabitExecutionProvider(),
                new ApprovalExecutionProvider(),
                new CustomExecutionProvider()
        );
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldValidateBooleanExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.BOOLEAN);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        Boolean value = state.get("value");
        assertThat(value).isFalse();
    }

    @Test
    void shouldValidateCountExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.COUNT);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        assertThat((Integer) state.get("current")).isEqualTo(0);
        assertThat((Integer) state.get("target")).isEqualTo(1);
    }

    @Test
    void shouldValidateTimerExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.TIMER);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        assertThat((Integer) state.get("elapsedSeconds")).isEqualTo(0);
        assertNull(state.get("targetSeconds"));
    }

    @Test
    void shouldValidateChecklistExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.CHECKLIST);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        assertNotNull(state.get("items"));
    }

    @Test
    void shouldValidateProgressExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.PROGRESS);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        Object percentage = state.get("percentage");
        assertThat(percentage).isEqualTo(0);
    }

    @Test
    void shouldValidateHabitExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.HABIT);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        Object streak = state.get("streak");
        assertThat(streak).isEqualTo(0);
    }

    @Test
    void shouldValidateApprovalExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.APPROVAL);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        Object status = state.get("status");
        assertThat(status).isEqualTo("pending");
    }

    @Test
    void shouldValidateCustomExecutionState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.CUSTOM);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        assertNotNull(state.get("data"));
    }

    @Test
    void shouldReturnEmptyStateForNullExecutionType() {
        Task task = createTaskWithExecutionType(null);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
        assertTrue(state.getData().isEmpty());
    }

    @Test
    void shouldSerializeStateToJson() {
        TaskExecutionState state = new TaskExecutionState("{\"test\": true}", objectMapper);
        String json = ExecutionStateValidation.serializeState(state);
        assertNotNull(json);
        assertThat(json).contains("test");
    }

    @Test
    void shouldDeserializeStateFromJson() {
        String json = "{\"key\": \"value\", \"number\": 42}";
        TaskExecutionState state = ExecutionStateValidation.deserializeState(json, objectMapper);
        assertNotNull(state);
        Object key = state.get("key");
        Object number = state.get("number");
        assertThat(key).isEqualTo("value");
        assertThat(number).isEqualTo(42);
    }

    @Test
    void shouldRoundTripSerializeDeserialize() {
        TaskExecutionState original = new TaskExecutionState("{\"key\": \"value\", \"count\": 10}", objectMapper);
        String json = ExecutionStateValidation.serializeState(original);
        TaskExecutionState restored = ExecutionStateValidation.deserializeState(json, objectMapper);
        Object key = restored.get("key");
        Object count = restored.get("count");
        assertThat(key).isEqualTo("value");
        assertThat(count).isEqualTo(10);
    }

    @Test
    void shouldHandleNullJsonInDeserialize() {
        TaskExecutionState state = ExecutionStateValidation.deserializeState(null, objectMapper);
        assertNotNull(state);
        assertTrue(state.getData().isEmpty());
    }

    @Test
    void shouldHandleEmptyJsonInDeserialize() {
        TaskExecutionState state = ExecutionStateValidation.deserializeState("", objectMapper);
        assertNotNull(state);
        assertTrue(state.getData().isEmpty());
    }

    @Test
    void shouldReturnValidExecutionType() {
        assertTrue(ExecutionStateValidation.isValidExecutionType(TaskExecutionType.BOOLEAN));
        assertTrue(ExecutionStateValidation.isValidExecutionType(TaskExecutionType.CUSTOM));
    }

    @Test
    void shouldReturnFalseForNullExecutionType() {
        assertFalse(ExecutionStateValidation.isValidExecutionType(null));
    }

    @Test
    void shouldGetStateData() {
        TaskExecutionState state = new TaskExecutionState("{\"a\": 1, \"b\": 2}", objectMapper);
        Map<String, Object> data = ExecutionStateValidation.getStateData(state);
        assertThat(data).hasSize(2);
        assertThat(data.get("a")).isEqualTo(1);
        assertThat(data.get("b")).isEqualTo(2);
    }

    @Test
    void shouldThrowForNullState() {
        assertThatThrownBy(() -> ExecutionStateValidation.validateStateNotNull(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Execution state cannot be null");
    }

    @Test
    void shouldAllowValidState() {
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertDoesNotThrow(() -> ExecutionStateValidation.validateStateNotNull(state));
    }

    @Test
    void shouldRejectInvalidBooleanState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.BOOLEAN);
        task.setExecutionState("{\"value\": \"not-a-bool\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidCountState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.COUNT);
        task.setExecutionState("{\"current\": -1, \"target\": 5}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidTimerState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.TIMER);
        task.setExecutionState("{\"elapsedSeconds\": -5}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidChecklistState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.CHECKLIST);
        task.setExecutionState("{\"items\": \"not-a-list\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidProgressState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.PROGRESS);
        task.setExecutionState("{\"percentage\": \"not-a-number\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidHabitState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.HABIT);
        task.setExecutionState("{\"streak\": \"not-a-number\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidApprovalState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.APPROVAL);
        task.setExecutionState("{\"status\": \"invalid-status\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidCustomState() {
        Task task = createTaskWithExecutionType(TaskExecutionType.CUSTOM);
        task.setExecutionState("{\"data\": \"not-a-map\"}");
        assertThatThrownBy(() -> ExecutionStateValidation.validateExecutionState(task, registry))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldHandleComplexStateSerialization() {
        String complexJson = "{\"items\": [{\"completed\": true}, {\"completed\": false}], \"completedCount\": 1, \"progress\": 50}";
        TaskExecutionState state = ExecutionStateValidation.deserializeState(complexJson, objectMapper);
        String serialized = ExecutionStateValidation.serializeState(state);
        assertNotNull(serialized);
        assertThat(serialized).contains("completedCount");
        assertThat(serialized).contains("progress");
    }

    @ParameterizedTest
    @EnumSource(TaskExecutionType.class)
    void shouldValidateAllExecutionTypes(TaskExecutionType type) {
        Task task = createTaskWithExecutionType(type);
        TaskExecutionState state = ExecutionStateValidation.validateExecutionState(task, registry);
        assertNotNull(state);
    }

    private Task createTaskWithExecutionType(TaskExecutionType type) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setUserId(UUID.randomUUID());
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.DRAFT);
        task.setPriority(TaskPriority.NORMAL);
        task.setVisibility(TaskVisibility.PRIVATE);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task.setExecutionType(type);
        if (type != null) {
            TaskExecutionProvider provider = registry.getProvider(type);
            task.setExecutionState(provider.initialize(task).toJson());
        }
        return task;
    }
}
