package org.example.taskmanager;

import org.example.taskmanager.dto.JobImageRequest;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.entity.JobImage;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.repo.JobImageRepository;
import org.example.taskmanager.service.JobImageService;
import org.example.taskmanager.service.TaskRunService;
import org.example.taskmanager.service.TaskService;
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
  @Autowired private TaskService taskService;
  @Autowired private TaskRunService taskRunService;
  @Autowired private JobImageService jobImageService;
  @Autowired private JobImageRepository jobImageRepository;

  // A jobImage that exists as dockerImage
  private static final String JOB_IMAGE_NAME = "worker-counter:v2";

  private TaskRequest request(String payload) {
    JobImage image = jobImageRepository.findByImageName(JOB_IMAGE_NAME).orElseGet(
        () -> jobImageService.createJobImage(new JobImageRequest(JOB_IMAGE_NAME)));
    return new TaskRequest("real-k8s-task", payload, Priority.HIGH, image.getId());
  }

  @Test
  void createAndExecuteTask_realKubernetes() {
    TaskRequest request = request("{\"durationSeconds\": 10, \"monkey\": \"balls\"}");
    Task task = taskService.createTask(request);
    Task result = taskRunService.execTask(task.getId());
    assertThat(result.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
  }

  @Test
  void createAndExecuteTask_task_which_takes_extremely_long() {
    TaskRequest request = request("{\"durationSeconds\": 3600, \"monkey\": \"balls\"}");
    Task task = taskService.createTask(request);
    Task result = taskRunService.execTask(task.getId());
    assertThat(result.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
  }
}


