package org.example.taskmanager.kafka;

import org.example.taskmanager.entity.type.TaskStatus;

import java.util.UUID;

public record TaskStatusEvent(UUID taskReferenceId, TaskStatus status, String errorMessage) {
	public TaskStatusEvent(UUID taskReferenceId, TaskStatus status) {
		this(taskReferenceId, status, null);
	}
}