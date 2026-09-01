package org.example.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.repo.TaskHistoryRepository;
import org.example.taskmanager.repo.TaskRepository;
import org.example.taskmanager.service.KubernetesService;
import org.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
Integration test, which adds data to our local db
Loads full-context @SpringBootTest
proves: controller → service → repository → real database all work together correctly.
MockMvc creates a mock HTTP request and sends it directly into Spring's MVC framework (skipping e.g. Tomcat).

KubernetesService is @MockBean'd — we don't want a real cluster call in a DB integration test,
and mocking it lets us deterministically drive the SUBMITTED vs FAILED execution paths.
*/
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(value = "classpath:integration.properties")
@ActiveProfiles("integration")
class TaskExecutionIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private TaskRepository taskRepository;
	@Autowired private TaskHistoryRepository taskHistoryRepository;
	@Autowired private TaskService taskService; // used directly to force a COMPLETED task, no public endpoint exists for this

	@MockBean private KubernetesService kubernetesService;

	@BeforeEach
	void cleanDb() {
		// task_history has ON DELETE CASCADE from task, but clearing both explicitly keeps intent obvious
		taskHistoryRepository.deleteAll();
		taskRepository.deleteAll();
	}

	@Test
	void createPendingTask() throws Exception {
		// Arrange
		TaskRequest request = new TaskRequest("pending-task", "{}", Priority.HIGH);

		// Act + Assert
		mockMvc.perform(post("/api/v1/tasks")
				                .contentType(MediaType.APPLICATION_JSON)
				                .content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("pending-task"))
				.andExpect(jsonPath("$.status").value("PENDING"));

		assertThat(taskRepository.findAll()).hasSize(1);
		assertThat(taskRepository.findAll().get(0).getStatus()).isEqualTo(TaskStatus.PENDING);
	}

	@Test
	void createAndCompleteTask() {
		// Arrange — no public endpoint drives a task to COMPLETED (that would presumably come from
		// a worker callback outside this API's current scope), so we go through the real service directly.
		TaskRequest request = new TaskRequest("completed-task", "{}", Priority.MEDIUM);
		Task created = taskService.createTask(request);

		// Act
		taskService.updateTaskStatus(created.getId(), new TaskStatusUpdate(TaskStatus.COMPLETED));

		// Assert
		Task result = taskService.getTask(created.getId());
		assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(result.getFinishedAt()).isNotNull();
	}

	@Test
	void createAndExecuteTask_kubernetesFails_resultsInFailedTask() throws Exception {
		// Arrange
		doThrow(new RuntimeException("cluster unreachable"))
				.when(kubernetesService)
				.createJob(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());

		UUID referenceId = UUID.randomUUID();
		TaskRequest request = new TaskRequest(referenceId, "failed-task", "{}", Priority.LOW);

		// Act — POST /api/v1/execute creates + executes in one call (createTaskAndExecute)
		mockMvc.perform(post("/api/v1/execute")
				                .contentType(MediaType.APPLICATION_JSON)
				                .content(objectMapper.writeValueAsString(request)))
				.andExpect(status().is5xxServerError()); // ADJUST: depends on your exception handler mapping
		// for the rethrown RuntimeException — replace with
		// the actual status your @ControllerAdvice returns

		// Assert — status persisted as FAILED despite the endpoint call itself erroring
		Task result = taskRepository.findByReferenceId(referenceId).orElseThrow();
		assertThat(result.getStatus()).isEqualTo(TaskStatus.FAILED);
		assertThat(taskHistoryRepository.findByTaskId(result.getId()))
				.anyMatch(h -> h.getStatus() == TaskStatus.FAILED && "cluster unreachable".equals(h.getErrorMessage()));
	}

	@Test
	void executeFailedTask_again_reExecutesSuccessfully() throws Exception {
		// Arrange — first attempt fails
		doThrow(new RuntimeException("cluster unreachable"))
				.when(kubernetesService)
				.createJob(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());

		UUID referenceId = UUID.randomUUID();
		TaskRequest request = new TaskRequest(referenceId, "retry-task", "{}", Priority.HIGH);

		mockMvc.perform(post("/api/v1/execute")
				                .contentType(MediaType.APPLICATION_JSON)
				                .content(objectMapper.writeValueAsString(request)))
				.andExpect(status().is5xxServerError()); // same ADJUST note as above

		Task afterFirstAttempt = taskRepository.findByReferenceId(referenceId).orElseThrow();
		assertThat(afterFirstAttempt.getStatus()).isEqualTo(TaskStatus.FAILED);

		// Arrange — second attempt: kubernetes now succeeds
		doNothing().when(kubernetesService)
				.createJob(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any());

		// Act — createTaskAndExecute called again with the SAME referenceId.
		// getTaskByReferenceId finds the existing (FAILED) task, so this re-executes rather than
		// creating a duplicate — and since FAILED != COMPLETED, TaskAlreadyCompletedException is not thrown.
		mockMvc.perform(post("/api/v1/execute")
				                .contentType(MediaType.APPLICATION_JSON)
				                .content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SUBMITTED"));

		// Assert
		assertThat(taskRepository.findAll()).hasSize(1); // still only one task — retry, not a duplicate
		Task afterRetry = taskRepository.findByReferenceId(referenceId).orElseThrow();
		assertThat(afterRetry.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
		assertThat(taskHistoryRepository.findByTaskId(afterRetry.getId())).hasSize(2); // one FAILED, one SUBMITTED entry
	}
}