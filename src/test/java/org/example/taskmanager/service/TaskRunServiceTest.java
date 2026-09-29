package org.example.taskmanager.service;

import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskAlreadyCompletedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskRunServiceTest {
	@Mock private TaskService taskService;
	@Mock private TaskHistoryService taskHistoryService;
	@Mock private KubernetesService kubernetesService;
	private TaskRunService taskRunService;

	@BeforeEach
	void setUp() {
		// Constructed manually instead of relying on @InjectMocks — @Value("${task.execution.worker-image}")
		// is a Spring-only mechanism, and there's no Spring context here, so it would resolve to null,
		// which breaks anyString() matchers in the verify() calls below (anyString() rejects null).
		// it does not really matter because all the dependencies of executionService are mocked,
		// so it would not start e.g a real kubernetes job
		taskRunService = new TaskRunService(
				taskService, taskHistoryService, kubernetesService, "worker-counter:v1", "192.168.5.15:9093");
	}

	private Task pendingTask() {
		return Task.builder()
				       .id(1L)
				       .referenceId(UUID.randomUUID())
				       .name("task1")
				       .status(TaskStatus.PENDING)
				       .build();
	}

	@Test
	void execTask_success() {
		// Arrange
		Task task = pendingTask();
		when(taskService.getTask(1L)).thenReturn(task);

		// Act
		Task result = taskRunService.execTask(1L);

		// Assert
		verify(kubernetesService).createJob(eq(task.getName() + "-" + task.getReferenceId()), anyString(), any());
		verify(taskService, never()).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.FAILED, null));
		assertThat(result).isEqualTo(task);
	}

	@Test
	void execTask_alreadyCompleted() {
		// Arrange
		Task task = pendingTask();;
		task.setStatus(TaskStatus.COMPLETED);
		when(taskService.getTask(1L)).thenReturn(task);

		// Act + Assert
		assertThrows(TaskAlreadyCompletedException.class, () -> taskRunService.execTask(1L));
		verify(kubernetesService, never()).createJob(anyString(), anyString(), any());
		verify(taskService, never()).updateTaskStatus(any(), any());
		verify(taskHistoryService, never()).createHistory(any(), any());
	}

	@Test
	void execTask_fail() {
		// Arrange
		Task task = pendingTask();;
		when(taskService.getTask(1L)).thenReturn(task);
		doThrow(new RuntimeException("k8s down"))
				.when(kubernetesService).createJob(anyString(), anyString(), any());

		// Act + Assert
		RuntimeException ex = assertThrows(RuntimeException.class, () -> taskRunService.execTask(1L));
		assertThat(ex.getMessage()).isEqualTo("k8s down");

		verify(taskService).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.FAILED, "k8s down"));
		verify(taskHistoryService).createHistory(1L, new TaskHistoryRequest(TaskStatus.FAILED, "k8s down"));
		verify(taskService, never()).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.PENDING));
	}

	@Test
	void cancelTask_taskId_notFound() {
		// Arrange
		Long notExistingTaskId = 9999L;
		when(taskService.getTask(notExistingTaskId))
				.thenThrow(new ResourceNotFoundException("Task not found with id: " + notExistingTaskId));

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskRunService.cancelTask(notExistingTaskId));
		verify(kubernetesService, never()).deleteJobsByName(anyString());
	}

	@Test
	void cancelTask_taskId_exist() {
		// Arrange
		Task existing = pendingTask();
		String jobName = existing.getName() + "-" + existing.getReferenceId();
		when(taskService.getTask(existing.getId())).thenReturn(existing);

		// Act
		taskRunService.cancelTask(existing.getId());

		// Assert
		verify(taskService).updateTaskStatus(
				existing.getId(), new TaskStatusUpdate(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
		verify(taskHistoryService).createHistory(
				existing.getId(), new TaskHistoryRequest(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
		verify(kubernetesService).deleteJobsByName(jobName);
	}
}