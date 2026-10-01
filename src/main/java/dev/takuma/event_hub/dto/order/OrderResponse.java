package dev.takuma.event_hub.dto.order;

import dev.takuma.event_hub.entity.Order;
import java.math.BigDecimal;

public record OrderResponse(
		Long id,
		String buyerName,
		String buyerEmail,
		Order.Status status,
		Long userId,
		Long ticketTypeId,
		int quantity,
		BigDecimal amount
) {

	public static OrderResponse from(Order order) {
		Long userId = order.getUser() == null ? null : order.getUser().getId();
		Long ticketTypeId = order.getTicketType() == null ? null : order.getTicketType().getId();
		return new OrderResponse(order.getId(), order.getBuyerName(), order.getBuyerEmail(), order.getStatus(),
				userId, ticketTypeId, order.getQuantity(), order.getAmount());
	}

}
