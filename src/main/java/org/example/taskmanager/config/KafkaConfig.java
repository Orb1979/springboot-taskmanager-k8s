package org.example.taskmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
	// override config if needed
	// or this class can be removed since Kafka is a autoconfigured.

	// Spring Boot ships a class called KafkaAutoConfiguration.
	// This class is annotated with something like @ConditionalOnClass(KafkaTemplate.class)
	// meaning: "only activate me if KafkaTemplate is actually present on the classpath."
}
