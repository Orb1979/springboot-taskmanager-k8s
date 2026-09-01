package org.example.taskmanager.repo;

import org.example.taskmanager.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

	List<TaskHistory> findByTaskId(Long taskId);

	Optional<TaskHistory> findByIdAndTaskId(Long id, Long taskId);

	TaskHistory findLatestHistoryByTaskId(Long taskId);

// same as above but with @Query
//	@Query("select th from TaskHistory th where th.task.id = :taskId order by th.createdAt DESC limit 1")
//	TaskHistory findLatestHistory(@Param("taskId") Long taskId);
}
