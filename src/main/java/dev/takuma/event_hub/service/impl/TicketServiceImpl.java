package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.entity.Ticket;
import dev.takuma.event_hub.repository.TicketRepository;
import dev.takuma.event_hub.service.QrService;
import dev.takuma.event_hub.service.TicketService;
import dev.takuma.event_hub.utils.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {

	private final TicketRepository ticketRepository;
	private final QrService qrService;

	public TicketServiceImpl(TicketRepository ticketRepository, QrService qrService) {
		this.ticketRepository = ticketRepository;
		this.qrService = qrService;
	}

	@Override
	@Transactional(readOnly = true)
	public TicketResponse findByCode(String code, String viewerEmail) {
		Ticket ticket = ApiException.orNotFound(
				ticketRepository.findByCode(code).filter(found -> found.visibleTo(viewerEmail)), "Ticket not found");
		ticket.attachQr(qrService.base64(ticket.getCode()));
		return TicketResponse.from(ticket);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TicketResponse> findVisibleTo(String viewerEmail) {
		List<Ticket> tickets = ticketRepository.findVisibleTo(viewerEmail);
		List<TicketResponse> responses = new ArrayList<>(tickets.size());
		for (Ticket ticket : tickets) {
			ticket.attachQr(qrService.base64(ticket.getCode()));
			responses.add(TicketResponse.from(ticket));
		}
		return responses;
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] qr(String code) {
		ApiException.orNotFound(ticketRepository.findByCode(code), "Ticket not found");
		return qrService.png(code);
	}

	@Override
	@Transactional
	public TicketResponse checkIn(String code, String sellerEmail) {
		Ticket ticket = ApiException.orNotFound(ticketRepository.findByCodeForUpdate(code), "Ticket not found");
		if (!ticket.checkableBy(sellerEmail)) {
			throw ApiException.forbidden("Ticket belongs to another seller's event");
		}
		if (ticket.isCheckedIn()) {
			throw ApiException.conflict("Ticket already used");
		}
		if (!ticket.isIssued()) {
			throw ApiException.conflict("Ticket is not valid");
		}
		ticket.markCheckedIn();
		return TicketResponse.from(ticketRepository.save(ticket));
	}

}
