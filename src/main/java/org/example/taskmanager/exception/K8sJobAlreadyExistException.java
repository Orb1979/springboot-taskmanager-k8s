package org.example.taskmanager.exception;

public class K8sJobAlreadyExistException extends RuntimeException {

		public K8sJobAlreadyExistException(String message) {
				super(message);
		}
}
