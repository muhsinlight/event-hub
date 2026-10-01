package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.ticket.TicketTypeRequest;
import dev.takuma.event_hub.dto.ticket.TicketTypeResponse;
import java.util.List;

public interface TicketTypeService {

	TicketTypeResponse add(Long eventId, String sellerEmail, TicketTypeRequest request);

	List<TicketTypeResponse> findByEventId(Long eventId, String viewerEmail);

}
