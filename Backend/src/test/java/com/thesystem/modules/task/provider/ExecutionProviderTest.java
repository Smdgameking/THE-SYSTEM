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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ExecutionProviderTest {

    private ObjectMapper objectMapper;
    private Task task;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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
    void shouldInitializeBooleanProvider() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((Boolean) state.get("value")).isFalse();
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.BOOLEAN);
    }

    @Test
    void shouldCalculateBooleanProgressWhenTrue() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"value\": true}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldCalculateBooleanProgressWhenFalse() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"value\": false}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(0);
    }

    @Test
    void shouldCalculateBooleanProgressWhenNullValue() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(0);
    }

    @Test
    void shouldReturnCompleteForBooleanTrue() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"value\": true}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldReturnIncompleteForBooleanFalse() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"value\": false}", objectMapper);
        assertThat(provider.isComplete(task, state)).isFalse();
    }

    @Test
    void shouldThrowValidationForMissingValueField() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldThrowValidationForNonBooleanValue() {
        BooleanExecutionProvider provider = new BooleanExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"value\": \"not-a-bool\"}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeCountProvider() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((Integer) state.get("current")).isEqualTo(0);
        assertThat((Integer) state.get("target")).isEqualTo(1);
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.COUNT);
    }

    @Test
    void shouldCalculateCountProgress() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"current\": 3, \"target\": 10}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(30);
    }

    @Test
    void shouldReturnCompleteWhenCountReachesTarget() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"current\": 10, \"target\": 10}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldReturnIncompleteWhenCountBelowTarget() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"current\": 5, \"target\": 10}", objectMapper);
        assertThat(provider.isComplete(task, state)).isFalse();
    }

    @Test
    void shouldThrowValidationForNegativeCurrent() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"current\": -1, \"target\": 10}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldThrowValidationForZeroOrNegativeTarget() {
        CountExecutionProvider provider = new CountExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"current\": 0, \"target\": 0}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeTimerProvider() {
        TimerExecutionProvider provider = new TimerExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((Integer) state.get("elapsedSeconds")).isEqualTo(0);
        assertNull(state.get("targetSeconds"));
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.TIMER);
    }

    @Test
    void shouldCalculateTimerProgress() {
        TimerExecutionProvider provider = new TimerExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"elapsedSeconds\": 30, \"targetSeconds\": 60}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(50);
    }

    @Test
    void shouldReturnCompleteWhenTimerReachesTarget() {
        TimerExecutionProvider provider = new TimerExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"elapsedSeconds\": 60, \"targetSeconds\": 60}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldThrowValidationForNegativeElapsedSeconds() {
        TimerExecutionProvider provider = new TimerExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"elapsedSeconds\": -1, \"targetSeconds\": 60}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeChecklistProvider() {
        ChecklistExecutionProvider provider = new ChecklistExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertNotNull(state.get("items"));
        assertThat(((java.util.List<?>) state.get("items"))).isEmpty();
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.CHECKLIST);
    }

    @Test
    void shouldCalculateChecklistProgress() {
        ChecklistExecutionProvider provider = new ChecklistExecutionProvider();
        String json = "[{\"completed\": true}, {\"completed\": false}, {\"completed\": true}]";
        TaskExecutionState state = new TaskExecutionState("{\"items\": " + json + "}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(66);
        assertThat((Integer) state.get("completedCount")).isEqualTo(2);
    }

    @Test
    void shouldReturnCompleteWhenAllChecklistItemsCompleted() {
        ChecklistExecutionProvider provider = new ChecklistExecutionProvider();
        String json = "[{\"completed\": true}, {\"completed\": true}]";
        TaskExecutionState state = new TaskExecutionState("{\"items\": " + json + "}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldReturnIncompleteWhenChecklistHasUncompletedItems() {
        ChecklistExecutionProvider provider = new ChecklistExecutionProvider();
        String json = "[{\"completed\": true}, {\"completed\": false}]";
        TaskExecutionState state = new TaskExecutionState("{\"items\": " + json + "}", objectMapper);
        assertThat(provider.isComplete(task, state)).isFalse();
    }

    @Test
    void shouldThrowValidationForMissingItemsField() {
        ChecklistExecutionProvider provider = new ChecklistExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeProgressProvider() {
        ProgressExecutionProvider provider = new ProgressExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((Integer) state.get("percentage")).isEqualTo(0);
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.PROGRESS);
    }

    @Test
    void shouldCalculateProgressWithPercentage() {
        ProgressExecutionProvider provider = new ProgressExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"percentage\": 75}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(75);
    }

    @Test
    void shouldClampProgressBetween0And100() {
        ProgressExecutionProvider provider = new ProgressExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"percentage\": 150}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldReturnCompleteWhenPercentageIs100() {
        ProgressExecutionProvider provider = new ProgressExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"percentage\": 100}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldThrowValidationForMissingPercentageField() {
        ProgressExecutionProvider provider = new ProgressExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeHabitProvider() {
        HabitExecutionProvider provider = new HabitExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((Integer) state.get("streak")).isEqualTo(0);
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.HABIT);
    }

    @Test
    void shouldCalculateHabitProgressWithStreak() {
        HabitExecutionProvider provider = new HabitExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"streak\": 5}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldReturnCompleteWhenStreakGreaterThanZero() {
        HabitExecutionProvider provider = new HabitExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"streak\": 3}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldThrowValidationForMissingStreakField() {
        HabitExecutionProvider provider = new HabitExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldThrowValidationForNegativeStreak() {
        HabitExecutionProvider provider = new HabitExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"streak\": -1}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeApprovalProvider() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertThat((String) state.get("status")).isEqualTo("pending");
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.APPROVAL);
    }

    @Test
    void shouldCalculateApprovalProgressForApproved() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"status\": \"approved\"}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldCalculateApprovalProgressForRejected() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"status\": \"rejected\"}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(0);
    }

    @Test
    void shouldReturnCompleteForApprovedOrRejected() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState approved = new TaskExecutionState("{\"status\": \"approved\"}", objectMapper);
        TaskExecutionState rejected = new TaskExecutionState("{\"status\": \"rejected\"}", objectMapper);
        assertThat(provider.isComplete(task, approved)).isTrue();
        assertThat(provider.isComplete(task, rejected)).isTrue();
    }

    @Test
    void shouldReturnIncompleteForPendingApproval() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"status\": \"pending\"}", objectMapper);
        assertThat(provider.isComplete(task, state)).isFalse();
    }

    @Test
    void shouldThrowValidationForInvalidApprovalStatus() {
        ApprovalExecutionProvider provider = new ApprovalExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"status\": \"invalid\"}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldInitializeCustomProvider() {
        CustomExecutionProvider provider = new CustomExecutionProvider();
        TaskExecutionState state = provider.initialize(task);
        assertThat(state).isNotNull();
        assertNotNull(state.get("data"));
        assertThat(provider.getType()).isEqualTo(TaskExecutionType.CUSTOM);
    }

    @Test
    void shouldCalculateCustomProgressWhenCompleted() {
        CustomExecutionProvider provider = new CustomExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"completed\": true}", objectMapper);
        state = provider.calculateProgress(task, state);
        assertThat((Integer) state.get("progress")).isEqualTo(100);
    }

    @Test
    void shouldReturnCompleteForCustomWhenCompletedTrue() {
        CustomExecutionProvider provider = new CustomExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{\"completed\": true}", objectMapper);
        assertThat(provider.isComplete(task, state)).isTrue();
    }

    @Test
    void shouldThrowValidationForMissingDataField() {
        CustomExecutionProvider provider = new CustomExecutionProvider();
        TaskExecutionState state = new TaskExecutionState("{}", objectMapper);
        assertThatThrownBy(() -> provider.validate(task, state))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }
}
