package org.example.taskmanager.kafka;

import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
	@InjectMocks private TaskStatusEventListener listener;

	@Test
	void onTaskStatusEvent_null_ignored() {
		listener.onTaskStatusEvent(null);

		verify(taskService, never()).getTaskByReferenceId(any());
		verify(taskService, never()).applyWorkerStatus(any(), any());
	}

	@Test
	void onTaskStatusEvent_unknownTask_ignored() {
		UUID refId = UUID.randomUUID();
		when(taskService.getTaskByReferenceId(refId)).thenReturn(Optional.empty());

		listener.onTaskStatusEvent(new TaskStatusEvent(refId, TaskStatus.RUNNING));

		verify(taskService, never()).applyWorkerStatus(any(), any());
	}

	@Test
	void onTaskStatusEvent_knownTask_delegatesToApplyWorkerStatus() {
		UUID refId = UUID.randomUUID();
		Task task = Task.builder().id(1L).referenceId(refId).status(TaskStatus.RUNNING).build();
		when(taskService.getTaskByReferenceId(refId)).thenReturn(Optional.of(task));
		when(taskService.applyWorkerStatus(1L, new TaskStatusUpdate(TaskStatus.COMPLETED, "boom")))
				.thenReturn(true);

		listener.onTaskStatusEvent(new TaskStatusEvent(refId, TaskStatus.COMPLETED, "boom"));

		verify(taskService).applyWorkerStatus(1L, new TaskStatusUpdate(TaskStatus.COMPLETED, "boom"));
	}
}
