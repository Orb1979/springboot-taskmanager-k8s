package org.example.taskmanager.exception;

public class TaskInvalidException extends RuntimeException {

	public TaskInvalidException(String message) {
		super(message);
	}

	public TaskInvalidException(String message, Throwable cause) {
		super(message, cause);
	}
}
