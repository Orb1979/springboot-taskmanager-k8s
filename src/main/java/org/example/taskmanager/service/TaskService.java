package org.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.TaskHistory;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskInvalidException;
import org.example.taskmanager.repo.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
	private final TaskRepository taskRepository;
	private final ObjectMapper objectMapper;

	public Optional<Task> getTaskByReferenceId(UUID referenceId){
		return taskRepository.findByReferenceId(referenceId);
	}

	@Transactional(readOnly = true)
	public Task getTask(Long id) {
		// A plain findById leaves history as an uninitialized proxy — fine inside the transaction, broken once it's over.
		return taskRepository.findWithHistoryById(id)
				       .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
	}

	@Transactional(readOnly = true)
	public List<Task> getTasks() {
		// A plain findAll leaves history as an uninitialized proxy — fine inside the transaction, broken once it's over.
		return taskRepository.findAllWithHistoryBy();
	}

	public Task createTask(TaskRequest request) {
    Task task =
        Task.builder()
            .name(request.name())
            .priority(request.priority())
            .payload(normalizePayload(request.payload()))
            .status(TaskStatus.PENDING)
            .build();
		task.addHistory(TaskHistory.builder().status(TaskStatus.PENDING).build());
		return taskRepository.save(task);
	}

	public Task updateTask(Long id, TaskRequest request) {
		Task task = getTask(id);
		if (request.name() != null) {
			task.setName(request.name());
		}
		if (request.payload() != null) {
			task.setPayload(normalizePayload(request.payload()));
		}
		if (request.priority() != null) {
			task.setPriority(request.priority());
		}
		return taskRepository.save(task);
	}

	public Task updateTaskStatus(Long id, TaskStatusUpdate update) {
		// we can update to any state, atm there is no allowed-transition check (not a state machine)
		Task task = getTask(id);
		task.setStatus(update.status());
		if (isTerminal(update.status())) {
			task.setFinishedAt(LocalDateTime.now());
		} else {
			task.setFinishedAt(null);
		}
		return taskRepository.save(task);
	}

	private String normalizePayload(String payload) {
		if (payload == null || payload.isBlank()) {
			return null;
		}
		try {
			objectMapper.readTree(payload);
			return payload;
		} catch (JacksonException exception) {
			throw new TaskInvalidException("Payload must contain valid JSON", exception);
		}
	}

	public void deleteTask(Long id) {
		taskRepository.delete(findTaskOrThrow(id));
	}

	private boolean isTerminal(TaskStatus status) {
		return status == TaskStatus.COMPLETED
				       || status == TaskStatus.FAILED
				       || status == TaskStatus.CANCELED;
	}

	private Task findTaskOrThrow(Long id) {
		return taskRepository.findById(id)
				       .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
	}
}
