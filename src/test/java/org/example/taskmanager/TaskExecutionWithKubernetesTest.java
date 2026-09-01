package org.example.taskmanager;

import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/*
Integration test, which adds data to our local db
Loads full-context @SpringBootTest
proves: service → repository → real database all work together correctly.
Start real kubernetes jobs
*/

@SpringBootTest
@TestPropertySource(value = "classpath:integration.properties")
@ActiveProfiles("integration")
class TaskExecutionWithKubernetesTest {
  @Autowired private ExecutionService executionService;

  @Test
  void createAndExecuteTask_realKubernetes() {
    TaskRequest request = new TaskRequest("real-k8s-task", "{\"durationSeconds\": 10, \"monkey\": \"balls\"}", Priority.HIGH);
    Task result = executionService.createTaskAndExecute(request);
    assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
  }

  @Test
  void createAndExecuteTask_task_which_takes_extremely_long() {
    TaskRequest request = new TaskRequest("real-k8s-task", "{\"durationSeconds\": 3600, \"monkey\": \"balls\"}", Priority.HIGH);
    Task result = executionService.createTaskAndExecute(request);

    assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
  }
}


