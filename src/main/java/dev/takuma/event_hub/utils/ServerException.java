package dev.takuma.event_hub.utils;

import org.springframework.http.HttpStatus;

public class ServerException extends ApiException {

	public ServerException(String message, Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, message);
		initCause(cause);
	}

	public static ServerException failure(Throwable cause) {
		return new ServerException("Server error", cause);
	}

}
