package org.example.taskmanager.dto;

public record JobImageRequest(String imageName, String description) {

	public JobImageRequest(String imageName) {
		this(imageName, null);
	}
}
