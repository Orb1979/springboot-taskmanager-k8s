package org.example.taskmanager.web;

import org.example.taskmanager.exception.K8sJobAlreadyExistException;
import org.example.taskmanager.exception.ResourceNotFoundException;
import org.example.taskmanager.exception.TaskAlreadyCompletedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
Domain exceptions now return an HTTP status that matches the failure instead of
a generic 500 response. This advice applies to all controller endpoints and can
also be used in sliced controller tests.

Sliced controller tests usually start without a web server. If they do not load
the @RestControllerAdvice, exceptions cannot be mapped to their intended HTTP
statuses. MockMvc lets the exception escape as a ServletException, so the test
never receives an HTTP response.
*/

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler({
			TaskAlreadyCompletedException.class,
			K8sJobAlreadyExistException.class
	})
	public ProblemDetail handleConflict(RuntimeException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
