package dev.takuma.event_hub.dto.ticket;

import dev.takuma.event_hub.entity.Ticket;

public record TicketResponse(
		Long id,
		String code,
		Ticket.Status status,
		Long orderId,
		Long ticketTypeId,
		String qr
) {

	public static TicketResponse from(Ticket ticket) {
		return new TicketResponse(ticket.getId(), ticket.getCode(), ticket.getStatus(), ticket.getOrder().getId(),
				ticket.getTicketType().getId(), ticket.getQr());
	}

}
