package org.example.taskmanager.web;

import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskNonStartableStateException;
import org.example.taskmanager.service.KubernetesService;
import org.example.taskmanager.service.TaskRunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/*
This is a Controller test.
loads a sliced Spring context, not the full Spring Boot application context.

@WebMvcTest:
Only instantiates the web layer:
Auto-configures MockMvc for you.
Excludes @Service, @Repository, @Component beans, and does not start an embedded server
Excludes the persistence layer entirely — no DataSource, no JPA/Hibernate auto-configuration

You're NOT testing:
 - service logic
 - repository
 - SQL

You're testing that the controller:
  - maps URLs correctly
  - accepts requests
  - calls the service
  - returns the correct HTTP status
  - returns the correct JSON
*/

@WebMvcTest(TaskRunController.class)
@Import(ApiExceptionHandler.class)
class TaskRunControllerTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;

	@MockitoBean private TaskRunService taskRunService;
	@MockitoBean private KubernetesService kubernetesService;

	private Task sampleTask(TaskStatus status) {
		return Task.builder()
				       .id(1L)
				       .referenceId(UUID.randomUUID())
				       .name("some-task-name")
				       .status(status)
				       .priority(Priority.HIGH)
				       .build();
	}

	@Test
	void executeExistingTask() throws Exception {
		// Arrange
		Task submitted = sampleTask(TaskStatus.PENDING);
		when(taskRunService.execTask(1L)).thenReturn(submitted);

		// Act + Assert
		mockMvc
				.perform(post("/api/v1/execute/1"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void executeTask_alreadyCompleted_returnsConflict() throws Exception {
		when(taskRunService.execTask(anyLong()))
				.thenThrow(new TaskNonStartableStateException("Task with reference id: x is already completed"));

		mockMvc
				.perform(post("/api/v1/execute/1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Task with reference id: x is already completed"));
	}

	@Test
	void executeTask_missingTask_returnsNotFound() throws Exception {
		when(taskRunService.execTask(anyLong()))
				.thenThrow(new ResourceNotFoundException("Task not found with id: 1"));

		mockMvc
				.perform(post("/api/v1/execute/1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Task not found with id: 1"));
	}

	@Test
	void executeTask_invalidArgument_returnsBadRequest() throws Exception {
		when(taskRunService.execTask(anyLong()))
				.thenThrow(new IllegalArgumentException("Payload must contain valid JSON"));

		mockMvc
				.perform(post("/api/v1/execute/1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Payload must contain valid JSON"));
	}
}