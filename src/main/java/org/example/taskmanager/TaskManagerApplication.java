package org.example.taskmanager;

import org.example.taskmanager.service.KubernetesService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TaskManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskManagerApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(KubernetesService kubernetesService) {
		return args -> {
			// You can add some initial tasks to the database here if needed
			// kubernetesService.createJob("initial-job", "worker-counter:v1", builder -> builder);
		};
	}

}
