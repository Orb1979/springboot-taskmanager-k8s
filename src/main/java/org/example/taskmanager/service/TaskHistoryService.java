package org.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.TaskHistory;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.repo.TaskHistoryRepository;
import org.example.taskmanager.repo.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskHistoryService {
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskHistory> getHistoryForTask(Long taskId) {
        ensureTaskExists(taskId);
        return taskHistoryRepository.findByTaskId(taskId);
    }

    public TaskHistory getHistory(Long taskId, Long historyId) {
        return findHistoryOrThrow(taskId, historyId);
    }

    public TaskHistory createHistory(Long taskId, TaskHistoryRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        TaskHistory history = TaskHistory.builder()
                .task(task)
                .status(request.status())
                .errorMessage(request.errorMessage())
                .build();

        return taskHistoryRepository.save(history);
    }

    public void deleteHistory(Long taskId, Long historyId) {
        taskHistoryRepository.delete(findHistoryOrThrow(taskId, historyId));
    }

    private void ensureTaskExists(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
    }

    private TaskHistory findHistoryOrThrow(Long taskId, Long historyId) {
        return taskHistoryRepository.findByIdAndTaskId(historyId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TaskHistory not found with id: " + historyId + " for task: " + taskId));
    }
}
