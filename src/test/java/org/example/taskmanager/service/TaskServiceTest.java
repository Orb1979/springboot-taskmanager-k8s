package org.example.taskmanager.service;


import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.JobImage;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskInvalidException;
import org.example.taskmanager.repo.JobImageRepository;
import org.example.taskmanager.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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
  @Mock private JobImageRepository jobImageRepository;
  @InjectMocks private TaskService taskService;

	@BeforeEach
	void setUp() {
		taskService = new TaskService(taskRepository, jobImageRepository, new ObjectMapper());
	}

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
		TaskRequest req = new TaskRequest("task1", "{}", Priority.HIGH);
		when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    // Act
		Task result = taskService.createTask(req);
	  // Assert
		assertEquals(TaskStatus.PENDING, result.getStatus());
		assertEquals(req.name(), result.getName());
		assertEquals(req.payload(), result.getPayload());
		assertEquals(req.priority(), result.getPriority());
		assertThat(result.getHistory()).hasSize(1);
		assertEquals(TaskStatus.PENDING, result.getHistory().getFirst().getStatus());
	}

	@Test
	void createTask_withImage() {
		JobImage image = JobImage.builder()
				.id(5L)
				.imageName("worker-counter:v2")
				.build();
		when(jobImageRepository.findById(5L)).thenReturn(Optional.of(image));
		when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Task result = taskService.createTask(new TaskRequest("task1", "{}", Priority.HIGH, 5L));

		assertThat(result.getImage()).isEqualTo(image);
	}

	@Test
	void createTask_imageNotFound() {
		when(jobImageRepository.findById(9999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> taskService.createTask(new TaskRequest("task1", "{}", Priority.HIGH, 9999L)));
		verify(taskRepository, never()).save(any());
	}

	@Test
	void createTask_normalizesName_throwException_on_name_is_null() {
		assertThrows(TaskInvalidException.class, ()-> taskService.createTask(new TaskRequest(null, "{}", Priority.HIGH)));
	}

	@Test
	void createTask_normalizesName_trim() {
		when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Task result = taskService.createTask(new TaskRequest(" trimmed ", "{}", Priority.HIGH));
		assertEquals("trimmed", result.getName());
	}

	@Test
	void createTask_with_empty_payload() {
		// Arrange
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
		// Act
		Task result1 = taskService.createTask(new TaskRequest("task", "", Priority.HIGH));
		Task result2 = taskService.createTask(new TaskRequest("task", "  ", Priority.HIGH));
		// Assert
		assertThat(result1.getPayload()).isNull();
		assertThat(result2.getPayload()).isNull();
		verify(taskRepository).save(result1);
		verify(taskRepository).save(result2);
	}

	@Test
	void createTask_with_invalidPayload_throwsException() {
		// Arrange
		TaskRequest req = new TaskRequest("task", "not valid json", Priority.HIGH);
		// Act + Assert
		assertThrows(TaskInvalidException.class,() -> taskService.createTask(req));
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void updateTask_onlyOverwritesNonNullFields() {
		// Arrange
		Task existing = Task.builder()
				                .id(1L)
				                .name("old")
				                .payload("{}")
				                .priority(Priority.LOW)
				                .build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		TaskRequest request = new TaskRequest("new", null, null);
		// Act
		Task result = taskService.updateTask(1L, request);
		// Assert
		assertEquals("new", result.getName());
		assertEquals("{}", result.getPayload());
		assertEquals(Priority.LOW, result.getPriority());
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
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		TaskRequest request = new TaskRequest("new", "{\"x\":1}", Priority.HIGH);
		// Act
		Task result = taskService.updateTask(1L, request);
		// Assert
		assertEquals("new", result.getName());
		assertEquals("{\"x\":1}", result.getPayload());
		assertEquals(Priority.HIGH, result.getPriority());
	}

	@Test
	void updateTask_with_emptyPayload() {
		// Arrange
		Task existing = Task.builder()
				                .id(1L)
				                .payload("{}")
				                .build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
		// Act
		Task result = taskService.updateTask(1L, new TaskRequest("task", "", Priority.HIGH));
		// Assert
		assertThat(result.getPayload()).isNull();
		verify(taskRepository).save(existing);
	}

	@Test
	void updateTask_with_invalidPayload_throwsException() {
		// Arrange
		Task existing = Task.builder()
				                .id(1L)
				                .payload("{}")
				                .build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		// Act + Assert
		assertThrows(
				TaskInvalidException.class,
				() -> taskService.updateTask(1L, new TaskRequest("task", "not valid json", Priority.HIGH)));
		assertThat(existing.getPayload()).isEqualTo("{}");
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void updateTask_notFound() {
		// Arrange
		when(taskRepository.findWithHistoryById(9999L)).thenReturn(Optional.empty());
		TaskRequest request = new TaskRequest("name", null, null);
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(9999L, request));
	}

	@Test
	void updateTask_setsImage() {
		JobImage image = JobImage.builder()
				.id(5L)
				.imageName("worker-counter:v2")
				.build();
		Task existing = Task.builder().id(1L).name("task").build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		when(jobImageRepository.findById(5L)).thenReturn(Optional.of(image));
		when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Task result = taskService.updateTask(1L, new TaskRequest(null, null, null, 5L));

		assertThat(result.getImage()).isEqualTo(image);
	}

	@Test
	void updateTaskStatus_setsStatus() {
		// Arrange
		Task existing = Task.builder().id(1L).status(TaskStatus.PENDING).build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
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
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));
		// Act
		taskService.updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.COMPLETED));
		// Assert
		assertEquals(TaskStatus.COMPLETED, existing.getStatus());
		assertThat(existing.getFinishedAt()).isNotNull();
	}

	@Test
	void updateTaskStatus_notFound() {
		// Arrange
		when(taskRepository.findWithHistoryById(9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class,
				() -> taskService.updateTaskStatus(9999L, new TaskStatusUpdate(TaskStatus.RUNNING)));
	}

	@Test
	void updateTaskStatus_clearsFinishedAt_whenNonTerminal() {
		Task existing = Task.builder()
				                .id(1L)
				                .status(TaskStatus.CANCELED)
				                .finishedAt(LocalDateTime.now())
				                .build();
		when(taskRepository.findWithHistoryById(1L)).thenReturn(Optional.of(existing));

		taskService.updateTaskStatus(1L, new TaskStatusUpdate(TaskStatus.PENDING));

		assertEquals(TaskStatus.PENDING, existing.getStatus());
		assertThat(existing.getFinishedAt()).isNull();
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
