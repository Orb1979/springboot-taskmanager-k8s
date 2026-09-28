package org.example.taskmanager.repo;

import org.example.taskmanager.entity.JobImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobImageRepository extends JpaRepository<JobImage, Long> {
	Optional<JobImage> findByImageName(String imageName);
}

