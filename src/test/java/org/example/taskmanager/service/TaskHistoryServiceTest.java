package org.example.taskmanager.service;

import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.TaskHistory;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.repo.TaskHistoryRepository;
import org.example.taskmanager.repo.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceTest {
	@Mock private TaskHistoryRepository taskHistoryRepository;
	@Mock private TaskRepository taskRepository;
	@InjectMocks private TaskHistoryService taskHistoryService;

	@Test
	void getHistoryForTask() {
		// Arrange
		TaskHistory history1 = TaskHistory.builder().id(1L).status(TaskStatus.PENDING).build();
		TaskHistory history2 = TaskHistory.builder().id(2L).status(TaskStatus.COMPLETED).build();
		when(taskRepository.existsById(1L)).thenReturn(true);
		when(taskHistoryRepository.findByTaskId(1L)).thenReturn(List.of(history1, history2));
		// Act
		List<TaskHistory> result = taskHistoryService.getHistoryForTask(1L);
		// Assert
		assertThat(result).containsExactly(history1, history2);
	}

	@Test
	void getHistoryForTask_taskNotFound() {
		// Arrange
		when(taskRepository.existsById(9999L)).thenReturn(false);
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.getHistoryForTask(9999L));
		verify(taskHistoryRepository, never()).findByTaskId(any());
	}

	@Test
	void getHistory() {
		// Arrange
		TaskHistory history = TaskHistory.builder().id(1L).status(TaskStatus.PENDING).build();
		when(taskHistoryRepository.findByIdAndTaskId(1L, 1L)).thenReturn(Optional.of(history));
		// Act
		TaskHistory result = taskHistoryService.getHistory(1L, 1L);
		// Assert
		assertThat(result).isEqualTo(history);
	}

	@Test
	void getHistory_notFound() {
		// Arrange
		when(taskHistoryRepository.findByIdAndTaskId(1L, 9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.getHistory(9999L, 1L));
	}

	@Test
	void createHistory() {
		// Arrange
		Task task = Task.builder().id(1L).build();
		TaskHistoryRequest request = new TaskHistoryRequest(TaskStatus.PENDING);
		when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
		when(taskHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		// Act
		TaskHistory result = taskHistoryService.createHistory(1L, request);
		// Assert
		assertEquals(TaskStatus.PENDING, result.getStatus());
		assertEquals(task, result.getTask());
		assertThat(result.getErrorMessage()).isNull();
	}

	@Test
	void createHistory_withErrorMessage() {
		// Arrange
		Task task = Task.builder().id(1L).build();
		TaskHistoryRequest request = new TaskHistoryRequest(TaskStatus.FAILED, "boom");
		when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
		when(taskHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		// Act
		TaskHistory result = taskHistoryService.createHistory(1L, request);
		// Assert
		assertEquals(TaskStatus.FAILED, result.getStatus());
		assertEquals("boom", result.getErrorMessage());
	}

	@Test
	void createHistory_taskNotFound() {
		// Arrange
		when(taskRepository.findById(9999L)).thenReturn(Optional.empty());
		TaskHistoryRequest request = new TaskHistoryRequest(TaskStatus.PENDING);
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.createHistory(9999L, request));
		verify(taskHistoryRepository, never()).save(any());
	}


	@Test
	void deleteHistory() {
		// Arrange
		TaskHistory existing = TaskHistory.builder().id(1L).build();
		when(taskHistoryRepository.findByIdAndTaskId(1L, 1L)).thenReturn(Optional.of(existing));
		// Act
		taskHistoryService.deleteHistory(1L, 1L);
		// Assert
		verify(taskHistoryRepository).delete(existing);
	}

	@Test
	void deleteHistory_notFound() {
		// Arrange
		when(taskHistoryRepository.findByIdAndTaskId(1L, 9999L)).thenReturn(Optional.empty());
		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.deleteHistory(9999L, 1L));
		verify(taskHistoryRepository, never()).delete(any());
	}
}