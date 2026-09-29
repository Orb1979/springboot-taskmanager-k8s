package org.example.taskmanager.kafka;

import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.service.TaskHistoryService;
import org.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskStatusEventListenerTest {
	@Mock private TaskService taskService;
	@Mock private TaskHistoryService taskHistoryService;
	@InjectMocks private TaskStatusEventListener listener;

	@Test
	void onTaskStatusEvent_null_ignored() {
		listener.onTaskStatusEvent(null);

		verify(taskService, never()).getTaskByReferenceId(any());
		verify(taskService, never()).updateTaskStatus(any(), any());
		verify(taskHistoryService, never()).createHistory(any(), any());
	}

	@Test
	void onTaskStatusEvent_unknownTask_ignored() {
		UUID refId = UUID.randomUUID();
		when(taskService.getTaskByReferenceId(refId)).thenReturn(Optional.empty());

		listener.onTaskStatusEvent(new TaskStatusEvent(refId, TaskStatus.RUNNING));

		verify(taskService, never()).updateTaskStatus(any(), any());
		verify(taskHistoryService, never()).createHistory(any(), any());
	}

	@ParameterizedTest
	@EnumSource(value = TaskStatus.class, names = {"CANCELED", "FAILED", "COMPLETED"})
	void onTaskStatusEvent_terminal_ignored(TaskStatus status) {
		UUID refId = UUID.randomUUID();
		Task task = Task.builder().id(1L).referenceId(refId).status(status).build();
		when(taskService.getTaskByReferenceId(refId)).thenReturn(Optional.of(task));

		listener.onTaskStatusEvent(new TaskStatusEvent(refId, TaskStatus.RUNNING));

		verify(taskService, never()).updateTaskStatus(any(), any());
		verify(taskHistoryService, never()).createHistory(any(), any());
	}

	@Test
	void onTaskStatusEvent_updatesStatusAndHistory() {
		UUID refId = UUID.randomUUID();
		Task task = Task.builder().id(1L).referenceId(refId).status(TaskStatus.PENDING).build();
		when(taskService.getTaskByReferenceId(refId)).thenReturn(Optional.of(task));

		listener.onTaskStatusEvent(new TaskStatusEvent(refId, TaskStatus.RUNNING, null));

		verify(taskService).updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.RUNNING, null));
		verify(taskHistoryService).createHistory(1L, new TaskHistoryRequest(TaskStatus.RUNNING, null));
	}
}
