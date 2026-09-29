package org.example.taskmanager.dto;

import org.example.taskmanager.entity.type.Priority;

public record TaskRequest(
				String name,
				String payload,
        Priority priority
) {}


