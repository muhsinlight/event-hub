package dev.takuma.event_hub.dto.ticket;

import dev.takuma.event_hub.entity.TicketType;
import java.math.BigDecimal;

public record TicketTypeResponse(
		Long id,
		String name,
		BigDecimal price,
		int quota,
		int soldCount,
		Long eventId
) {

	public static TicketTypeResponse from(TicketType ticketType) {
		return new TicketTypeResponse(ticketType.getId(), ticketType.getName(), ticketType.getPrice(),
				ticketType.getQuota(), ticketType.getSoldCount(), ticketType.getEvent().getId());
	}

}
