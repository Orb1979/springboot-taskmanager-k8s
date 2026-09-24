package org.example.taskmanager.service;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import lombok.extern.log4j.Log4j2;
import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskAlreadyCompletedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Log4j2
@Service
public class TaskRunService {
	private final TaskService taskService;
	private final TaskHistoryService taskHistoryService;
  private final KubernetesService kubernetesService;
  private final String workerImage;
  private final String kafkaBootstrapServers;

  public TaskRunService(
      TaskService taskService,
      TaskHistoryService taskHistoryService,
      KubernetesService kubernetesService,
      @Value("${task.execution.worker-image}") String workerImage,
      @Value("${task.execution.kafka-bootstrap-servers}") String kafkaBootstrapServers
  ) {
    this.taskService = taskService;
    this.taskHistoryService = taskHistoryService;
    this.kubernetesService = kubernetesService;
    this.workerImage = workerImage;
    this.kafkaBootstrapServers = kafkaBootstrapServers;
  }

  public Task createAndExecuteTask(TaskRequest taskRequest) {
    return execTask(resolveTask(taskRequest));
  }

  public Task execTask(Long taskId) {
    return execTask(taskService.getTask(taskId));
  }

  public void cancelTask(Long taskId) {
    Task task = taskService.getTask(taskId);
    taskService.updateTaskStatus(
        task.getId(), new TaskStatusUpdate(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
    taskHistoryService.createHistory(
        task.getId(), new TaskHistoryRequest(TaskStatus.CANCELED, TaskStatus.CANCELED.toString()));
    kubernetesService.deleteJobsByName(getJobName(task));
  }

  private Task resolveTask(TaskRequest request) {
    UUID referenceId = request.referenceId();
    if (referenceId == null) {
      return taskService.createTask(request);
    }
    return taskService.getTaskByReferenceId(referenceId)
               .orElseThrow(() -> new ResourceNotFoundException(
                   "Task with reference id: %s not found".formatted(referenceId)));
  }

  private String getJobName(Task task) {
    return task.getName() + "-" + task.getReferenceId();
  }

  private Task execTask(Task task) {
    if (task.getStatus() == TaskStatus.COMPLETED) {
      throw new TaskAlreadyCompletedException(
          "Task with reference id: %s is already completed".formatted(task.getReferenceId()));
    }

    String jobName = getJobName(task);
    try {
      kubernetesService.createJob(jobName, workerImage, withTaskEnv(task));
    } catch (Exception e) {
      taskService.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.FAILED, e.getMessage()));
      taskHistoryService.createHistory(task.getId(), new TaskHistoryRequest(TaskStatus.FAILED, e.getMessage()));
      throw e;
    }
    return taskService.getTask(task.getId());
  }

  private Function<ContainerBuilder, ContainerBuilder> withTaskEnv(Task task) {
    // adding Task payload to the config builder (keeping kubernetesService decoupled from Task)
    // Task.payload is a non required field in the db, it will get normalized to null if blank
    // when passing the value as env variable through kubernetes, pass an empty json string, when its null
    String taskPayload = Objects.requireNonNullElse(task.getPayload(), "{}");
    return builder -> builder
      .addNewEnv().withName("TASK_ID").withValue(String.valueOf(task.getId())).endEnv()
      .addNewEnv().withName("TASK_PAYLOAD").withValue(taskPayload).endEnv()
      .addNewEnv().withName("TASK_REFERENCE_ID").withValue(task.getReferenceId().toString()).endEnv()
      .addNewEnv().withName("KAFKA_BOOTSTRAP_SERVERS").withValue(kafkaBootstrapServers).endEnv();
  }
}