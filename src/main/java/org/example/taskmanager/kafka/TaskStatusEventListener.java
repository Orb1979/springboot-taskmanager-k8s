package org.example.taskmanager.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.taskmanager.dto.TaskStatusUpdate;
import org.example.taskmanager.entity.Task;
import org.example.taskmanager.service.TaskService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TaskStatusEventListener {

	private final TaskService taskService;

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

		TaskStatusUpdate taskStatusUpdate = new TaskStatusUpdate(event.status(), event.errorMessage());
		boolean applied = taskService.applyWorkerStatus(task.getId(), taskStatusUpdate);
		if (!applied) {
			log.warn("task-status-events: task {} is already terminal, ignoring event with status {}",
					task.getId(), event.status());
		}
	}
}