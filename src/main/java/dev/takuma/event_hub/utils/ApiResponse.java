package dev.takuma.event_hub.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponse<T> {

	private final int status;
	private final String message;
	private final T data;

	private ApiResponse(Builder<T> builder) {
		this.status = builder.status.value();
		this.message = builder.message;
		this.data = builder.data;
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public T getData() {
		return data;
	}

	public static class Builder<T> {

		private HttpStatus status = HttpStatus.OK;
		private String message = "OK";
		private T data;

		public Builder<T> status(HttpStatus status) {
			this.status = status;
			return this;
		}

		public Builder<T> message(String message) {
			this.message = message;
			return this;
		}

		public Builder<T> data(T data) {
			this.data = data;
			return this;
		}

		public ResponseEntity<ApiResponse<T>> response() {
			return ResponseEntity.status(status).body(new ApiResponse<>(this));
		}

	}

}
