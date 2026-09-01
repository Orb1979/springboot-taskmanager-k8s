package org.example.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.taskmanager.entity.type.TaskStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (status == null) {
			status = TaskStatus.PENDING;
		}
	}
}