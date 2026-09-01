package org.example.taskmanager.dto;

import org.example.taskmanager.entity.type.Priority;

import java.util.UUID;

public record TaskRequest(
				UUID referenceId,
				String name,
				String payload,
        Priority priority
) {

	public TaskRequest(String name, String payload, Priority priority) {
		this(null, name, payload, priority);
	}
}


