package org.example.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.taskmanager.entity.type.Priority;
import org.example.taskmanager.entity.type.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "task",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_task_reference_id", columnNames = "reference_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference_id", nullable = false, updatable = false)
  private UUID referenceId;

  @Column(nullable = false)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private String payload;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column private LocalDateTime finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @Builder.Default
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<TaskHistory> history = new ArrayList<>();

  public void addHistory(TaskHistory history) {
    history.setTask(this);
    this.history.add(history);
  }

  @PrePersist
  void onCreate() {
		if (referenceId == null) {
			referenceId = UUID.randomUUID();
		}

    createdAt = LocalDateTime.now();
    updatedAt = createdAt;

		if (status == null) {
			status = TaskStatus.PENDING;
		}
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
    if (status == TaskStatus.COMPLETED && finishedAt == null) {
      finishedAt = updatedAt;
    }
  }
}

/*
removed @Data, because it generates
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode > don't want task to be equal if the field are the same,

eg when the id is still null
Task task1 = new Task();
task1.setPriority(HIGH);

Task task2 = new Task();
task2.setPriority(HIGH);

Lombok may consider them equal if all fields are equal, even though they are different objects.
*/
