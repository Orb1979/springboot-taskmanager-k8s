package org.example.taskmanager.dto;

import org.example.taskmanager.entity.TaskHistory;
import org.example.taskmanager.entity.type.TaskStatus;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        Long id,
        Long taskId,
        LocalDateTime createdAt,
        TaskStatus status,
        String errorMessage
) {
    public static TaskHistoryResponse from(TaskHistory history) {
        return new TaskHistoryResponse(
                history.getId(),
                history.getTask().getId(),
                history.getCreatedAt(),
                history.getStatus(),
                history.getErrorMessage()
        );
    }
}
