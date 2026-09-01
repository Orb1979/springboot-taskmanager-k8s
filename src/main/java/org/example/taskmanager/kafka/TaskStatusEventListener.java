package org.example.taskmanager.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.taskmanager.dto.TaskHistoryRequest;
import org.example.taskmanager.dto.TaskStatusUpdate;
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

		if (event == null) {
			log.warn("Received empty/tombstone message on task-status-events, ignoring");
			return;
		}

		System.out.println(" >> Received task status event: " + event);
		taskService.updateTaskStatus(event.taskId(), new TaskStatusUpdate(event.status(), event.errorMessage()));
		taskHistoryService.createHistory(event.taskId(), new TaskHistoryRequest(event.status(), event.errorMessage()));
	}
}