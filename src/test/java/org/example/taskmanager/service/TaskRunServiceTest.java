package org.example.taskmanager.service;

import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.JobImage;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskInvalidException;
import org.example.taskmanager.exception.TaskNonStartableStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
		taskRunService = new TaskRunService(
				taskService, taskHistoryService, kubernetesService, "192.168.5.15:9093");
	}

	private Task pendingTask() {
		return Task.builder()
				       .id(1L)
				       .referenceId(UUID.randomUUID())
				       .name("task1")
				       .status(TaskStatus.PENDING)
				       .image(JobImage.builder().id(1L).imageName("worker-counter:v2").build())
				       .build();
	}

	@Test
	void execTask_success() {
		Task task = pendingTask();
		when(taskService.getTask(1L)).thenReturn(task);

		Task result = taskRunService.execTask(1L);

		verify(kubernetesService).createJob(eq(task.getReferenceId().toString()), eq("worker-counter:v2"), any());
		verify(taskService, never()).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.FAILED, null));
		assertThat(result).isEqualTo(task);
	}

	@Test
	void execTask_missingImage() {
		Task task = pendingTask();
		task.setImage(null);
		when(taskService.getTask(1L)).thenReturn(task);

		assertThrows(TaskInvalidException.class, () -> taskRunService.execTask(1L));
		verify(kubernetesService, never()).createJob(anyString(), anyString(), any());
	}

	@ParameterizedTest
	@EnumSource(
			value = TaskStatus.class,
			names = {"COMPLETED", "FAILED", "RUNNING", "CANCELED"}
	)
	void execTask_is_not_startable(TaskStatus status) {
		Task task = pendingTask();
		task.setStatus(status);
		when(taskService.getTask(1L)).thenReturn(task);

		assertThrows(TaskNonStartableStateException.class, () -> taskRunService.execTask(1L));
		verify(kubernetesService, never()).createJob(anyString(), anyString(), any());
		verify(taskService, never()).updateTaskStatus(any(), any());
		verify(taskHistoryService, never()).createHistory(any(), any());
	}

	@Test
	void execTask_fail() {
		Task task = pendingTask();
		when(taskService.getTask(1L)).thenReturn(task);
		doThrow(new RuntimeException("k8s down"))
				.when(kubernetesService).createJob(anyString(), anyString(), any());

		RuntimeException ex = assertThrows(RuntimeException.class, () -> taskRunService.execTask(1L));
		assertThat(ex.getMessage()).isEqualTo("k8s down");

		verify(taskService).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.FAILED, "k8s down"));
		verify(taskHistoryService).createHistory(1L, new TaskHistoryRequest(TaskStatus.FAILED, "k8s down"));
		verify(taskService, never()).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.PENDING));
	}

	@Test
	void cancelTask_taskId_notFound() {
		Long notExistingTaskId = 9999L;
		when(taskService.getTask(notExistingTaskId))
				.thenThrow(new ResourceNotFoundException("Task not found with id: " + notExistingTaskId));

		assertThrows(ResourceNotFoundException.class, () -> taskRunService.cancelTask(notExistingTaskId));
		verify(kubernetesService, never()).deleteJobsByName(anyString());
	}

	@Test
	void cancelTask_taskId_exist() {
		Task existing = pendingTask();
		when(taskService.getTask(existing.getId())).thenReturn(existing);

		taskRunService.cancelTask(existing.getId());

		verify(taskService).updateTaskStatus(
				existing.getId(), new TaskStatusUpdate(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
		verify(taskHistoryService).createHistory(
				existing.getId(), new TaskHistoryRequest(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
		verify(kubernetesService).deleteJobsByName(existing.getReferenceId().toString());
	}
}
