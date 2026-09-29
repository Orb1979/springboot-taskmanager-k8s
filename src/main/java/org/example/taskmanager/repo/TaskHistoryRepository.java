package org.example.taskmanager.repo;

import org.example.taskmanager.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

	List<TaskHistory> findByTaskId(Long taskId);

	Optional<TaskHistory> findByIdAndTaskId(Long id, Long taskId);
}
