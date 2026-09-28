package org.example.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "image_name", nullable = false, unique = true)
	private String imageName;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Builder.Default
	@OneToMany(mappedBy = "image", fetch = FetchType.LAZY)
	private List<Task> tasks = new ArrayList<>();
}
