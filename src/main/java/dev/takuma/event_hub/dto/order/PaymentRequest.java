package dev.takuma.event_hub.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
		@NotBlank @Pattern(regexp = "\\d{13,19}") String cardNumber,
		@NotBlank @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}") String expiry,
		@NotBlank @Pattern(regexp = "\\d{3,4}") String cvc,
		@NotBlank String code
) {
}
