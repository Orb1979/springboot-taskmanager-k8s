package org.example.taskmanager.dto;

import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    Long id,
    UUID referenceId,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime finishedAt,
    String payload,
    Priority priority,
    TaskStatus status,
    List<TaskHistoryResponse> taskHistory,
    JobImageResponse image
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getReferenceId(),
                task.getName(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getFinishedAt(),
                task.getPayload(),
                task.getPriority(),
                task.getStatus(),
                task.getHistory().stream().map(TaskHistoryResponse::from).toList(),
                task.getImage() == null ? null : JobImageResponse.from(task.getImage())
        );
    }
}
