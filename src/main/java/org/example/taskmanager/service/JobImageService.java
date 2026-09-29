package org.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.JobImageRequest;
import org.example.taskmanager.entity.JobImage;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskInvalidException;
import org.example.taskmanager.repo.JobImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobImageService {
	private final JobImageRepository jobImageRepository;

	@Transactional(readOnly = true)
	public List<JobImage> getJobImages() {
		return jobImageRepository.findAll();
	}

	@Transactional(readOnly = true)
	public JobImage getJobImage(Long id) {
		return findJobImageOrThrow(id);
	}

	public JobImage createJobImage(JobImageRequest request) {
		JobImage jobImage = JobImage.builder()
				                    .imageName(normalizeImageName(request.imageName()))
				                    .description(blankToNull(request.description()))
				                    .build();
		return jobImageRepository.save(jobImage);
	}

	public JobImage updateJobImage(Long id, JobImageRequest request) {
		JobImage existing = findJobImageOrThrow(id);
		if (request.imageName() != null) {
			existing.setImageName(normalizeImageName(request.imageName()));
		}
		if (request.description() != null) {
			existing.setDescription(blankToNull(request.description()));
		}
		return jobImageRepository.save(existing);
	}

	public void deleteJobImage(Long id) {
		jobImageRepository.delete(findJobImageOrThrow(id));
	}

	private JobImage findJobImageOrThrow(Long id) {
		return jobImageRepository.findById(id)
				       .orElseThrow(() -> new ResourceNotFoundException("Job image not found with id: " + id));
	}

	private String normalizeImageName(String imageName) {
		if (imageName == null || imageName.isBlank()) {
			throw new TaskInvalidException("Image name must not be blank");
		}
		return imageName.trim();
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
