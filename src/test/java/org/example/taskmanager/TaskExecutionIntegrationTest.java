package org.example.taskmanager;

import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.service.TaskRunService;
import org.example.taskmanager.service.KubernetesService;
import org.example.taskmanager.service.TaskHistoryService;
import org.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

/*
Integration test, which adds data to our local db
Loads full-context @SpringBootTest
proves: service → repository → real database all work together correctly.
KubernetesService is @MockBean, we don't want a real cluster call in a DB integration test,
*/

@SpringBootTest
@TestPropertySource(value = "classpath:integration.properties")
@ActiveProfiles("integration")
class TaskExecutionIntegrationTest {
  @Autowired private TaskRunService taskRunService;
  @Autowired private TaskService taskService;
  @Autowired private TaskHistoryService taskHistoryService;
  @MockitoBean private KubernetesService kubernetesService;

  @Test
  void createPendingTask() {
    TaskRequest request = new TaskRequest("pending-task", "{}", Priority.HIGH);
	  Task task = taskService.createTask(request);
	  taskRunService.execTask(task.getId());
  }

  @Test
  void createAndCompleteTask() {
    TaskRequest request = new TaskRequest("completed-task", "{}", Priority.HIGH);
	  Task task = taskService.createTask(request);
	  Task result = taskRunService.execTask(task.getId());
    taskService.updateTaskStatus(result.getId(), new TaskStatusUpdate(TaskStatus.COMPLETED));
  }

  @Test
  void createAndFailTask() {
    // Arrange
    TaskRequest request = new TaskRequest("failed-task", "{}", Priority.HIGH);
    doThrow(new RuntimeException("k8s down")).when(kubernetesService)
        .createJob(anyString(), anyString(), any());

    // Act + Assert
    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> {
	        Task task = taskService.createTask(request);
	        taskRunService.execTask(task.getId());
        });
    assertThat(ex).hasMessageContaining("k8s down");
  }

  @Test
  void executeFailedTask_again_reExecutesSuccessfully() {
    // Arrange
    TaskRequest request = new TaskRequest("task-first-attempt", "{}", Priority.HIGH);
		Task task = taskService.createTask(request);

		// run first time, it fails (updateTaskStatus would normally be called by kafka event)
		taskRunService.execTask(task.getId());
	  Task first = taskService.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.FAILED, "error"));
    taskHistoryService.createHistory(first.getId(), new TaskHistoryRequest(TaskStatus.FAILED, "error"));

		// task would need to be reset to pending, before it can start again
	  Task reset = taskService.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.PENDING, "error"));
	  taskHistoryService.createHistory(reset.getId(), new TaskHistoryRequest(TaskStatus.PENDING));

	  // run second time, it succeeds
	  taskRunService.execTask(task.getId());
	  Task reRun = taskService.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.COMPLETED));
	  taskHistoryService.createHistory(reRun.getId(), new TaskHistoryRequest(TaskStatus.COMPLETED));

    // Assert
    assertThat(first.getStatus()).isEqualTo(TaskStatus.FAILED);
	  assertThat(reset.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(reRun.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(reRun.getFinishedAt()).isNotNull();
    assertThat(reRun.getHistory().size()).isGreaterThan(1);
  }
}


