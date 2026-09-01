package org.example.taskmanager.dto;

import org.example.taskmanager.entity.type.TaskStatus;

public record TaskHistoryRequest(
        TaskStatus status,
        String errorMessage
) {
	public TaskHistoryRequest (TaskStatus status) {
		this(status, null);
	}
}
