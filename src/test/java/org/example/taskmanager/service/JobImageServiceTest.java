package org.example.taskmanager.service;

import org.example.taskmanager.dto.JobImageRequest;
import org.example.taskmanager.entity.JobImage;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskInvalidException;
import org.example.taskmanager.repo.JobImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobImageServiceTest {
	@Mock private JobImageRepository jobImageRepository;
	@InjectMocks private JobImageService jobImageService;

	@Test
	void getJobImages() {
		JobImage image = JobImage.builder().id(1L).imageName("worker-counter:v2").build();
		when(jobImageRepository.findAll()).thenReturn(List.of(image));

		List<JobImage> result = jobImageService.getJobImages();

		assertThat(result).containsExactly(image);
	}

	@Test
	void getJobImage() {
		JobImage image = JobImage.builder().id(1L).imageName("worker-counter:v2").build();
		when(jobImageRepository.findById(1L)).thenReturn(Optional.of(image));

		JobImage result = jobImageService.getJobImage(1L);

		assertThat(result).isEqualTo(image);
	}

	@Test
	void getJobImage_notFound() {
		when(jobImageRepository.findById(9999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> jobImageService.getJobImage(9999L));
	}

	@Test
	void createJobImage() {
		when(jobImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		JobImage result = jobImageService.createJobImage(new JobImageRequest("worker-counter:v2", "desc"));

		assertThat(result.getImageName()).isEqualTo("worker-counter:v2");
		assertThat(result.getDescription()).isEqualTo("desc");
	}

	@Test
	void createJobImage_blankName() {
		assertThrows(TaskInvalidException.class,
				() -> jobImageService.createJobImage(new JobImageRequest("  ", null)));
		verify(jobImageRepository, never()).save(any());
	}

	@Test
	void deleteJobImage() {
		JobImage image = JobImage.builder().id(1L).imageName("worker-counter:v2").build();
		when(jobImageRepository.findById(1L)).thenReturn(Optional.of(image));

		jobImageService.deleteJobImage(1L);

		verify(jobImageRepository).delete(image);
	}

	@Test
	void deleteJobImage_notFound() {
		when(jobImageRepository.findById(9999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> jobImageService.deleteJobImage(9999L));
		verify(jobImageRepository, never()).delete(any());
	}
}
