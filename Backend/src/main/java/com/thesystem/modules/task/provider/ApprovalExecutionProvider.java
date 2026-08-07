package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ApprovalExecutionProvider implements TaskExecutionProvider {

    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
            "pending", "approved", "rejected"
    );

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.APPROVAL;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState(
                "{\"status\": \"pending\", \"approvals\": [], \"rejections\": []}",
                new ObjectMapper()
        );
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        String status = state.get("status");
        if (status == null) {
            status = "pending";
        }
        int progress = switch (status) {
            case "approved" -> 100;
            case "rejected" -> 0;
            default -> 0;
        };
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        String status = state.get("status");
        return "approved".equals(status) || "rejected".equals(status);
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("status")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Approval execution state must contain 'status' field"
            );
        }
        String status = state.get("status");
        if (!VALID_STATUSES.contains(status)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Approval status must be one of: " + VALID_STATUSES
            );
        }
    }
}
