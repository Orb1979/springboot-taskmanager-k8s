package org.example.taskmanager.kafka;

import org.example.taskmanager.entity.type.TaskStatus;

public record TaskStatusEvent(Long taskId, TaskStatus status, String errorMessage) {
	public TaskStatusEvent(Long taskId, TaskStatus status) {
		this(taskId, status, null);
	}
}

// example kafkaMessage
// {"taskId": 5, "status": "RUNNING", "errorMessage": null}