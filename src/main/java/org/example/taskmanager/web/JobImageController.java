package org.example.taskmanager.web;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.JobImageRequest;
import org.example.taskmanager.dto.JobImageResponse;
import org.example.taskmanager.service.JobImageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-images")
@RequiredArgsConstructor
public class JobImageController {
	private final JobImageService jobImageService;

	@GetMapping
	public List<JobImageResponse> getJobImages() {
		return jobImageService.getJobImages().stream().map(JobImageResponse::from).toList();
	}

	@GetMapping("/{id}")
	public JobImageResponse getJobImage(@PathVariable Long id) {
		return JobImageResponse.from(jobImageService.getJobImage(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public JobImageResponse createJobImage(@RequestBody JobImageRequest request) {
		return JobImageResponse.from(jobImageService.createJobImage(request));
	}

	@PutMapping("/{id}")
	public JobImageResponse updateJobImage(@PathVariable Long id, @RequestBody JobImageRequest request) {
		return JobImageResponse.from(jobImageService.updateJobImage(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteJobImage(@PathVariable Long id) {
		jobImageService.deleteJobImage(id);
	}
}
