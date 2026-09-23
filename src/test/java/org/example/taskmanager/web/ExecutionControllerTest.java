package org.example.taskmanager.web;

import jakarta.servlet.ServletException;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.TaskAlreadyCompletedException;
import org.example.taskmanager.service.TaskRunService;
import org.example.taskmanager.service.KubernetesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {
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
	void createAndExecuteTask() throws Exception {
		// Arrange
		TaskRequest request = new TaskRequest("some-task-name", "{}", Priority.HIGH);
		Task task = sampleTask(TaskStatus.PENDING);
		when(taskRunService.createAndExecuteTask(any())).thenReturn(task);

		// Act + Assert
		mockMvc
				.perform(post("/api/v1/execute")
						         .contentType(MediaType.APPLICATION_JSON)
						         .content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("some-task-name"))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}


	@Test
	void executeExistingTask() throws Exception {
		// Arrange
		Task submitted = sampleTask(TaskStatus.PENDING);
		when(taskRunService.execTask(1L)).thenReturn(submitted);

		// Act + Assert
		mockMvc
				.perform(get("/api/v1/execute/1"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void executeTask_alreadyCompleted_returnsServerError() throws Exception {
		// Arrange
		when(taskRunService.execTask(anyLong()))
				.thenThrow(new TaskAlreadyCompletedException("Task with reference id: x is already completed"));

		// Act + Assert
		Exception ex = assertThrows(ServletException.class, () -> mockMvc.perform(get("/api/v1/execute/1")));
		assertThat(ex.getCause()).isInstanceOf(TaskAlreadyCompletedException.class);

		/* note:
		MockMvc has no real servlet container, so there's no container-level fallback to turn an
		unhandled RuntimeException into a 500 response the way a real deployed server would.
		With no @ControllerAdvice/@ExceptionHandler catching TaskAlreadyCompletedException, MockMvc
		just rethrows it out of .perform(...), wrapped in a ServletException.
		There's never an actual HTTP status produced here, so we can not do something like:
		mockMvc
				.perform(get("/api/v1/execute/1"))
				.andExpect(status().is5xxServerError());
		 */
	}
}