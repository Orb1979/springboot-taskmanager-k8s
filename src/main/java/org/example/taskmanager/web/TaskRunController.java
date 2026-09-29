package org.example.taskmanager.web;


import lombok.RequiredArgsConstructor;
import org.example.taskmanager.dto.TaskResponse;
import org.example.taskmanager.service.TaskRunService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
public class TaskRunController {
	private final TaskRunService taskRunService;

	@PostMapping("/{taskId}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public TaskResponse execute(@PathVariable Long taskId) {
		// execute a task, which we already created
		return TaskResponse.from(taskRunService.execTask(taskId));
	}

	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable Long taskId) {
		// cancel a task, which we already created
		taskRunService.cancelTask(taskId);
	}
}

