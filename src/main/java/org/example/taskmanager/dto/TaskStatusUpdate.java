package org.example.taskmanager.dto;

import org.example.taskmanager.entity.type.TaskStatus;

public record TaskStatusUpdate(TaskStatus status, String errorMessage) {
	public TaskStatusUpdate(TaskStatus status) {
		this(status, null);
	}
}