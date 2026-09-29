package org.example.taskmanager.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.entity.type.TaskStatus;
import org.example.taskmanager.service.TaskHistoryService;
import org.example.taskmanager.service.TaskService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TaskStatusEventListener {

	private final TaskService taskService;
	private final TaskHistoryService taskHistoryService;

	@KafkaListener(topics = "task-status-events", groupId = "task-manager")
	public void onTaskStatusEvent(TaskStatusEvent event) {
		log.info("task-status-events: received, event: {} ",event);

		if (event == null) {
			log.warn("task-status-events: event is empty, ignoring");
			return;
		}

		Task task = taskService.getTaskByReferenceId(event.taskReferenceId()).orElse(null);
		if (task == null) {
			log.warn("task-status-events: task not found, with reference ID {}, ignoring", event.taskReferenceId());
			return;
		}

		if (task.getStatus().equals(TaskStatus.CANCELED)) {
			log.warn("task-status-events: event for for cancelled task, ignoring event with status  {} ", event.status());
			return;
		}
		if (task.getStatus().equals(TaskStatus.FAILED)) {
			log.warn("task-status-events: event for for failed task, ignoring event with status  {} ", event.status());
			return;
		}
		if (task.getStatus().equals(TaskStatus.COMPLETED)) {
			log.warn("task-status-events: event for completed task, ignoring event with status {}", event.status());
			return;
		}

		taskService.updateTaskStatus(task.getId(), new TaskStatusUpdate(event.status(), event.errorMessage()));
		taskHistoryService.createHistory(task.getId(), new TaskHistoryRequest(event.status(), event.errorMessage()));
	}
}