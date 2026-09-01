package org.example.taskmanager.exception;

public class TaskAlreadyExistException extends RuntimeException {

		public TaskAlreadyExistException(String message) {
				super(message);
		}
}
