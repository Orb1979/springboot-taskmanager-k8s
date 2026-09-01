package org.example.taskmanager;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class LogEnv implements CommandLineRunner {
	@Value("${task.execution.kafka-bootstrap-servers}")
	private String kubernetesInternalIp;

	@Override
	public void run(String... args) throws Exception {
		log.info("Starting test environment");
		log.info("task.execution.kafka-bootstrap-servers: {}", kubernetesInternalIp);
	}
}

