package org.example.taskmanager.dto;

import org.example.taskmanager.entity.JobImage;

public record JobImageResponse(Long id, String imageName, String description) {

	public static JobImageResponse from(JobImage jobImage) {
		return new JobImageResponse(jobImage.getId(), jobImage.getImageName(), jobImage.getDescription());
	}
}
