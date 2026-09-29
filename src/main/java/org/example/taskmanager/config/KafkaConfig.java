package org.example.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Configuration
@EnableKafka
public class KafkaConfig {

	@Bean
	CommonErrorHandler kafkaErrorHandler() {
		ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
		backOff.setMaxInterval(30_000L);

		DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
		handler.defaultFalse();
		handler.addRetryableExceptions(
				TransientDataAccessException.class,
				RecoverableDataAccessException.class,
				DataAccessResourceFailureException.class,
				CannotCreateTransactionException.class,
				TransactionTimedOutException.class,
				SocketTimeoutException.class,
				TimeoutException.class);
		return handler;
	}
}
