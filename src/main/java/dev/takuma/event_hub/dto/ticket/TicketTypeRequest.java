package dev.takuma.event_hub.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TicketTypeRequest(
		@NotBlank String name,
		@NotNull @Positive BigDecimal price,
		@Positive int quota
) {
}
