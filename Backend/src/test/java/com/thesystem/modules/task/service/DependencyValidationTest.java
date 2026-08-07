package com.thesystem.modules.task.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.enums.DependencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DependencyValidationTest {

    private UUID taskA;
    private UUID taskB;
    private UUID taskC;
    private UUID taskD;
    private Map<UUID, List<UUID>> dependencyGraph;

    @BeforeEach
    void setUp() {
        taskA = UUID.randomUUID();
        taskB = UUID.randomUUID();
        taskC = UUID.randomUUID();
        taskD = UUID.randomUUID();
        dependencyGraph = new HashMap<>();
    }

    @Test
    void shouldAllowSelfDependencyCheck() {
        assertThatThrownBy(() -> DependencyValidation.validateNoSelfDependency(taskA, taskA))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    void shouldAllowDifferentTaskIds() {
        assertDoesNotThrow(() -> DependencyValidation.validateNoSelfDependency(taskA, taskB));
    }

    @Test
    void shouldDetectDuplicateDependency() {
        TaskDependency existing = new TaskDependency();
        existing.setTaskId(taskA);
        existing.setDependsOnTaskId(taskB);
        List<TaskDependency> existingDeps = List.of(existing);

        assertThatThrownBy(() -> DependencyValidation.validateNoDuplicate(taskA, taskB, existingDeps))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dependency already exists");
    }

    @Test
    void shouldAllowNewDependency() {
        TaskDependency existing = new TaskDependency();
        existing.setTaskId(taskA);
        existing.setDependsOnTaskId(taskC);
        List<TaskDependency> existingDeps = List.of(existing);

        assertDoesNotThrow(() -> DependencyValidation.validateNoDuplicate(taskA, taskB, existingDeps));
    }

    @Test
    void shouldAllowEmptyExistingDependencies() {
        assertDoesNotThrow(() -> DependencyValidation.validateNoDuplicate(taskA, taskB, List.of()));
    }

    @Test
    void shouldDetectSelfDependencyInCycleCheck() {
        assertThatThrownBy(() -> DependencyValidation.validateNoCycle(taskA, taskA, dependencyGraph))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldDetectDirectCycleAtoBtoA() {
        dependencyGraph.put(taskB, List.of(taskA));
        assertThatThrownBy(() -> DependencyValidation.validateNoCycle(taskA, taskB, dependencyGraph))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldDetectIndirectCycleAtoBtoCtoA() {
        dependencyGraph.put(taskB, List.of(taskC));
        dependencyGraph.put(taskC, List.of(taskA));
        assertThatThrownBy(() -> DependencyValidation.validateNoCycle(taskA, taskC, dependencyGraph))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldAllowNonCyclicDependency() {
        dependencyGraph.put(taskB, List.of(taskC));
        dependencyGraph.put(taskC, List.of());
        assertDoesNotThrow(() -> DependencyValidation.validateNoCycle(taskA, taskB, dependencyGraph));
    }

    @Test
    void shouldDetectCycleThroughLongerPath() {
        dependencyGraph.put(taskB, List.of(taskC));
        dependencyGraph.put(taskC, List.of(taskA));
        dependencyGraph.put(taskA, List.of(taskB));
        assertThatThrownBy(() -> DependencyValidation.validateNoCycle(taskB, taskC, dependencyGraph))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldReturnTrueForWouldCreateCycle() {
        dependencyGraph.put(taskB, List.of(taskA));
        assertThat(DependencyValidation.wouldCreateCycle(taskA, taskB, dependencyGraph)).isTrue();
    }

    @Test
    void shouldReturnFalseForNonCyclicDependency() {
        dependencyGraph.put(taskB, List.of(taskC));
        assertThat(DependencyValidation.wouldCreateCycle(taskA, taskB, dependencyGraph)).isFalse();
    }

    @Test
    void shouldReturnTrueForSelfDependency() {
        assertThat(DependencyValidation.wouldCreateCycle(taskA, taskA, dependencyGraph)).isTrue();
    }

    @Test
    void shouldThrowForNullDependencyType() {
        assertThatThrownBy(() -> DependencyValidation.validateDependencyType(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dependency type cannot be null");
    }

    @Test
    void shouldAllowValidDependencyType() {
        assertDoesNotThrow(() -> DependencyValidation.validateDependencyType(DependencyType.BLOCKS));
        assertDoesNotThrow(() -> DependencyValidation.validateDependencyType(DependencyType.RELATED));
    }

    @Test
    void shouldThrowForNullTasks() {
        assertThatThrownBy(() -> DependencyValidation.validateDependencyTasksExist(null, new Task()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Both tasks must exist");
        assertThatThrownBy(() -> DependencyValidation.validateDependencyTasksExist(new Task(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Both tasks must exist");
        assertThatThrownBy(() -> DependencyValidation.validateDependencyTasksExist(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Both tasks must exist");
    }

    @Test
    void shouldAllowValidTasks() {
        Task task1 = new Task();
        Task task2 = new Task();
        assertDoesNotThrow(() -> DependencyValidation.validateDependencyTasksExist(task1, task2));
    }

    @Test
    void shouldDetectCycleInComplexGraph() {
        UUID taskD = UUID.randomUUID();
        dependencyGraph.put(taskA, List.of(taskB));
        dependencyGraph.put(taskB, List.of(taskC));
        dependencyGraph.put(taskC, List.of(taskD));
        dependencyGraph.put(taskD, List.of(taskA));

        assertThatThrownBy(() -> DependencyValidation.validateNoCycle(taskA, taskD, dependencyGraph))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldHandleDiamondDependencyPattern() {
        dependencyGraph.put(taskB, List.of(taskD));
        dependencyGraph.put(taskC, List.of(taskD));
        dependencyGraph.put(taskD, List.of());
        assertDoesNotThrow(() -> DependencyValidation.validateNoCycle(taskA, taskB, dependencyGraph));
        assertDoesNotThrow(() -> DependencyValidation.validateNoCycle(taskA, taskC, dependencyGraph));
    }

    @Test
    void shouldHandleEmptyGraph() {
        assertDoesNotThrow(() -> DependencyValidation.validateNoCycle(taskA, taskB, dependencyGraph));
    }

    @Test
    void shouldHandleSelfReferencingNode() {
        dependencyGraph.put(taskA, List.of(taskA));
        assertThat(DependencyValidation.wouldCreateCycle(taskA, taskA, dependencyGraph)).isTrue();
    }
}
