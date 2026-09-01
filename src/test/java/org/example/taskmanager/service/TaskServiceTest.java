package org.example.taskmanager.service;


import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskAlreadyExistException;
import org.example.taskmanager.repo.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
  @Mock private TaskRepository taskRepository;
  @InjectMocks private TaskService taskService;

  @Test
  void getTaskByReferenceId() {
	  // Arrange
		UUID refId = UUID.randomUUID();
		Task task = Task.builder().referenceId(refId).build();
		when(taskRepository.findByReferenceId(refId)).thenReturn(Optional.of(task));
	  // Act
		Optional<Task> foundTask = taskService.getTaskByReferenceId(refId);
		// Assert
		assert foundTask.isPresent();
  }

  @Test
  void getTask() {
	  // Arrange
		Task task = Task.builder().id(1L).name("test").build();
	  when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(task));
		// Act
		Task result = taskService.getTask(1L);
	  // Assert
		assertThat(result).isEqualTo(task);
  }

	@Test
	void getTask_notFound() {
		// Arrange
		when(taskRepository.findWithHistoryById(9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, ()-> taskService.getTask(9999L));
	}

	@Test
  void getTasks() {
		// Arrange
		Task task1 = Task.builder().id(1L).name("task1").build();
		Task task2 = Task.builder().id(2L).name("task2").build();
		List<Task> tasks = List.of(task1, task2);
		when(taskRepository.findAllWithHistoryBy()).thenReturn(tasks);
		// ACT
		List<Task> result = taskService.getTasks();
		// Assert
		assertThat(result).isEqualTo(tasks);
	}

	@Test
	void createTask() {
		// Arrange
		TaskRequest taskrequest = new TaskRequest("task1", "{}", Priority.HIGH);
    Task savedTask =
        Task.builder()
            .name("task1")
            .payload("{}")
            .priority(Priority.HIGH)
            .status(TaskStatus.PENDING)
            .build();
		when(taskRepository.findByReferenceId(any())).thenReturn(Optional.empty());
		when(taskRepository.save(any())).thenReturn(savedTask);
    // Act
		Task result = taskService.createTask(taskrequest);
	  // Assert
		assertEquals(TaskStatus.PENDING, result.getStatus());
		assertEquals(taskrequest.name(), result.getName());
		assertEquals(taskrequest.payload(), result.getPayload());
		assertEquals(taskrequest.priority(), result.getPriority());
	}

  @Test
  void createTask_referenceId_already_exist() {
		// Arrange
	  UUID referenceId = UUID.randomUUID();
	  TaskRequest taskrequest = new TaskRequest(referenceId, "name", "{\"k\":1}", Priority.HIGH);
		when(taskRepository.findByReferenceId(referenceId)).thenReturn(Optional.of(new Task()));
	  // Act + Assert
		assertThrows(TaskAlreadyExistException.class, ()-> taskService.createTask(taskrequest));
  }

	@Test
	void updateTask_onlyOverwritesNonNullFields() {
		// Arrange
		Task existing = Task.builder()
				                .id(1L)
				                .name("old name")
				                .payload("{}")
				                .priority(Priority.LOW)
				                .build();
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		TaskRequest request = new TaskRequest("new name", null, null); // payload/priority omitted
		// Act
		Task result = taskService.updateTask(1L, request);
		// Assert
		assertEquals("new name", result.getName());
		assertEquals("{}", result.getPayload());       // unchanged — proves the null-guard works
		assertEquals(Priority.LOW, result.getPriority()); // unchanged
	}

	@Test
	void updateTask_overwritesAllFields_whenAllProvided() {
		// Arrange
		Task existing = Task.builder()
				                .id(1L)
				                .name("old")
				                .payload("{}")
				                .priority(Priority.LOW)
				                .build();
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		TaskRequest request = new TaskRequest("new name", "{\"x\":1}", Priority.HIGH);
		// Act
		Task result = taskService.updateTask(1L, request);
		// Assert
		assertEquals("new name", result.getName());
		assertEquals("{\"x\":1}", result.getPayload());
		assertEquals(Priority.HIGH, result.getPriority());
	}

	@Test
	void updateTask_notFound() {
		// Arrange
		when(taskRepository.findById(9999L)).thenReturn(Optional.empty());
		TaskRequest request = new TaskRequest("name", null, null);
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(9999L, request));
	}

	@Test
	void updateTaskStatus_setsStatus() {
		// Arrange
		Task existing = Task.builder().id(1L).status(TaskStatus.PENDING).build();
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		// Act
		taskService.updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.RUNNING));
		// Assert
		assertEquals(TaskStatus.RUNNING, existing.getStatus());
		assertThat(existing.getFinishedAt()).isNull();
	}

	@Test
	void updateTaskStatus_setsFinishedAt_whenTerminal() {
		// Arrange
		Task existing = Task.builder().id(1L).status(TaskStatus.RUNNING).build();
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		// Act
		taskService.updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.COMPLETED));
		// Assert
		assertEquals(TaskStatus.COMPLETED, existing.getStatus());
		assertThat(existing.getFinishedAt()).isNotNull();
	}

	@Test
	void updateTaskStatus_notFound() {
		// Arrange
		when(taskRepository.findById(9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class,
				() -> taskService.updateTaskStatus(9999L, new TaskStatusUpdate(TaskStatus.RUNNING)));
	}

	@Test
	void deleteTask_deletesTask() {
		// Arrange
		Task existing = Task.builder().id(1L).build();
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		// Act
		taskService.deleteTask(1L);
		// Assert
		verify(taskRepository).delete(existing);
	}

	@Test
	void deleteTask_notFound() {
		// Arrange
		when(taskRepository.findById(9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(9999L));
		verify(taskRepository, never()).delete(any());
	}
}
