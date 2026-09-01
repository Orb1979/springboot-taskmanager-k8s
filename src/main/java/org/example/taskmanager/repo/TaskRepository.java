package org.example.taskmanager.repo;

import org.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long> {

	Optional<Task> findByReferenceId(UUID referenceId);

	/*
	@EntityGraph
	For this query only, also fetch the specified association, as a join, in one round trip.
	This gives you the best of both:
	- the entity mapping stays lazy-by-default (safe, cheap, no accidental N+1 elsewhere),
	- the specific calls that need history populated (getTask, getTasks, feeding TaskResponse) get it eagerly in one shot

	It also sidesteps a subtler issue: a custom JOIN FETCH on a @OneToMany returns duplicate parent rows unless
	you add DISTINCT yourself — @EntityGraph handles that correctly without you having to think about it
  */

	@EntityGraph(attributePaths = "history")
	Optional<Task> findWithHistoryById(Long id);

	@EntityGraph(attributePaths = "history")
	List<Task> findAllWithHistoryBy();

	// similar result as findAllWithHistoryBy
	@Query("Select Distinct t from Task t LEFT Join Fetch t.history")
	List<Task> findAllWithHistory();

  // similar result as findAllWithHistoryBy
  @Query(
      value = "select distinct * from task t left join task_history th  on t.id = th.task_id",
      nativeQuery = true)
  List<Task> findAllWithHistoryNative();
}

