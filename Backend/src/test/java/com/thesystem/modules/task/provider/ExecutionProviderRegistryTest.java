package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExecutionProviderRegistryTest {

    private TaskExecutionProviderRegistry registry;
    private Task task;

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
        task = new Task();
        task.setId(UUID.randomUUID());
        task.setUserId(UUID.randomUUID());
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.DRAFT);
        task.setPriority(TaskPriority.NORMAL);
        task.setVisibility(TaskVisibility.PRIVATE);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldReturnBooleanProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.BOOLEAN);
        assertThat(provider).isInstanceOf(BooleanExecutionProvider.class);
    }

    @Test
    void shouldReturnCountProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.COUNT);
        assertThat(provider).isInstanceOf(CountExecutionProvider.class);
    }

    @Test
    void shouldReturnTimerProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.TIMER);
        assertThat(provider).isInstanceOf(TimerExecutionProvider.class);
    }

    @Test
    void shouldReturnChecklistProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.CHECKLIST);
        assertThat(provider).isInstanceOf(ChecklistExecutionProvider.class);
    }

    @Test
    void shouldReturnProgressProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.PROGRESS);
        assertThat(provider).isInstanceOf(ProgressExecutionProvider.class);
    }

    @Test
    void shouldReturnHabitProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.HABIT);
        assertThat(provider).isInstanceOf(HabitExecutionProvider.class);
    }

    @Test
    void shouldReturnApprovalProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.APPROVAL);
        assertThat(provider).isInstanceOf(ApprovalExecutionProvider.class);
    }

    @Test
    void shouldReturnCustomProvider() {
        TaskExecutionProvider provider = registry.getProvider(TaskExecutionType.CUSTOM);
        assertThat(provider).isInstanceOf(CustomExecutionProvider.class);
    }

    @ParameterizedTest
    @EnumSource(TaskExecutionType.class)
    void shouldReturnProviderForAllExecutionTypes(TaskExecutionType type) {
        TaskExecutionProvider provider = registry.getProvider(type);
        assertThat(provider).isNotNull();
        assertThat(provider.getType()).isEqualTo(type);
    }

    @Test
    void shouldThrowForUnknownExecutionType() {
        assertThrows(com.thesystem.common.exception.BusinessException.class, () -> registry.getProvider(null));
    }

    @Test
    void shouldDelegateGetProgressToProvider() {
        task.setExecutionType(TaskExecutionType.BOOLEAN);
        TaskExecutionState state = new TaskExecutionState("{\"value\": true}", new ObjectMapper());
        TaskExecutionState result = registry.getProgress(task, state);
        assertThat(result).isNotNull();
        assertThat((Integer) result.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldDelegateIsCompleteToProvider() {
        task.setExecutionType(TaskExecutionType.BOOLEAN);
        TaskExecutionState state = new TaskExecutionState("{\"value\": true}", new ObjectMapper());
        boolean complete = registry.isComplete(task, state);
        assertThat(complete).isTrue();
    }

    @Test
    void shouldReturnFalseForIncompleteState() {
        task.setExecutionType(TaskExecutionType.BOOLEAN);
        TaskExecutionState state = new TaskExecutionState("{\"value\": false}", new ObjectMapper());
        boolean complete = registry.isComplete(task, state);
        assertThat(complete).isFalse();
    }

    @Test
    void shouldThrowForGetProgressWithNullType() {
        task.setExecutionType(null);
        TaskExecutionState state = new TaskExecutionState("{}", new ObjectMapper());
        assertThrows(com.thesystem.common.exception.BusinessException.class, () -> registry.getProgress(task, state));
    }

    @Test
    void shouldThrowForIsCompleteWithNullType() {
        task.setExecutionType(null);
        TaskExecutionState state = new TaskExecutionState("{}", new ObjectMapper());
        assertThrows(com.thesystem.common.exception.BusinessException.class, () -> registry.isComplete(task, state));
    }
}
