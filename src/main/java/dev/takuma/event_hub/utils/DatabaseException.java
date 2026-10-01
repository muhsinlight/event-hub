package dev.takuma.event_hub.utils;

import org.springframework.http.HttpStatus;

public class DatabaseException extends ApiException {

	private DatabaseException(HttpStatus status, String message, Throwable cause) {
		super(status, message);
		initCause(cause);
	}

	public static DatabaseException conflict(Throwable cause) {
		return new DatabaseException(HttpStatus.CONFLICT, "Data conflict", cause);
	}

	public static DatabaseException failure(Throwable cause) {
		return new DatabaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", cause);
	}

}
