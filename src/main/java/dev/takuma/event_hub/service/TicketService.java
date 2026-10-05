package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.ticket.TicketResponse;
import java.util.List;

public interface TicketService {

	TicketResponse findByCode(String code, String viewerEmail);

	List<TicketResponse> findVisibleTo(String viewerEmail);

	byte[] qr(String code);

	TicketResponse checkIn(String code, String sellerEmail);

}
