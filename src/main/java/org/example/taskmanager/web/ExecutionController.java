package org.example.taskmanager.web;


import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
public class ExecutionController {
	private final ExecutionService executionService;

	@GetMapping("/{taskId}")
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse execute(@PathVariable Long taskId) {
		// execute a task, which we already created
		return TaskResponse.from(executionService.execTask(taskId));
	}

	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse createAndExecute(@RequestBody TaskRequest request) {
		// create a task and directly execute it
		return TaskResponse.from(executionService.createTaskAndExecute(request));
	}
}

