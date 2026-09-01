package org.example.taskmanager.web;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskHistoryResponse;
import org.example.taskmanager.service.TaskHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/history")
@RequiredArgsConstructor
public class TaskHistoryController {
    private final TaskHistoryService taskHistoryService;

  @GetMapping
  public List<TaskHistoryResponse> getHistory(@PathVariable Long taskId) {
    return taskHistoryService.getHistoryForTask(taskId).stream()
        .map(TaskHistoryResponse::from)
        .toList();
  }

    @GetMapping("/{historyId}")
    public TaskHistoryResponse getHistory(@PathVariable Long taskId, @PathVariable Long historyId) {
        return TaskHistoryResponse.from(taskHistoryService.getHistory(taskId, historyId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskHistoryResponse createHistory(@PathVariable Long taskId,
                                             @RequestBody TaskHistoryRequest request) {
        return TaskHistoryResponse.from(taskHistoryService.createHistory(taskId, request));
    }

    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable Long taskId, @PathVariable Long historyId) {
        taskHistoryService.deleteHistory(taskId, historyId);
    }
}
