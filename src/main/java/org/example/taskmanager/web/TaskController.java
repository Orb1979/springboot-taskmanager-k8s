package org.example.taskmanager.web;

import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
	private final TaskService taskService;

	@GetMapping
	public List<TaskResponse> getTasks() {
		return taskService.getTasks().stream().map(TaskResponse::from).toList();
	}

	@GetMapping("/{id}")
	public TaskResponse getTask(@PathVariable Long id) {
		return TaskResponse.from(taskService.getTask(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse createTask(@RequestBody TaskRequest request) {
			return TaskResponse.from(taskService.createTask(request));
	}

	@PutMapping("/{id}")
	public TaskResponse updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
		return TaskResponse.from(taskService.updateTask(id, request));
	}

	@PutMapping("/{id}/status")
	public TaskResponse updateTaskStatus(@PathVariable Long id, @RequestBody TaskStatusUpdate taskStatusUpdate) {
			return TaskResponse.from(taskService.updateTaskStatus(id, taskStatusUpdate));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTask(@PathVariable Long id) {
		taskService.deleteTask(id);
	}
}