package dev.takuma.event_hub.utils;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
		if (exception.getStatus().is5xxServerError()) {
			log.error(exception.getMessage(), exception);
		}
		return error(exception.getStatus(), exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return error(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
		DatabaseException failure = DatabaseException.conflict(exception);
		log.error(failure.getMessage(), exception);
		return error(failure.getStatus(), failure.getMessage());
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiResponse<Void>> handleDatabase(DataAccessException exception) {
		DatabaseException failure = DatabaseException.failure(exception);
		log.error(failure.getMessage(), exception);
		return error(failure.getStatus(), failure.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
		if (exception instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
			return error(status, status.getReasonPhrase());
		}
		ServerException failure = ServerException.failure(exception);
		log.error(failure.getMessage(), exception);
		return error(failure.getStatus(), failure.getMessage());
	}

	private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
		return ApiResponse.<Void>builder()
				.status(status)
				.message(message)
				.response();
	}

}
