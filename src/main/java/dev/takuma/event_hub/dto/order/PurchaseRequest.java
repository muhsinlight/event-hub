package dev.takuma.event_hub.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
		@NotNull Long ticketTypeId,
		@Positive int quantity
) {
}
