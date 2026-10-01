package dev.takuma.event_hub.web;

import java.util.stream.Collectors;
import org.springframework.validation.BindingResult;

final class Forms {

	private Forms() {
	}

	static String validationMessage(BindingResult binding) {
		return binding.getFieldErrors().stream()
				.map(error -> error.getField() + " " + (error.getDefaultMessage() == null ? "is invalid"
						: error.getDefaultMessage()))
				.collect(Collectors.joining(", "));
	}

}
