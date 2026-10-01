package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.ticket.TicketTypeRequest;
import dev.takuma.event_hub.dto.ticket.TicketTypeResponse;
import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.entity.TicketType;
import dev.takuma.event_hub.repository.TicketTypeRepository;
import dev.takuma.event_hub.service.TicketTypeService;
import dev.takuma.event_hub.service.support.EventAccess;
import dev.takuma.event_hub.utils.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketTypeServiceImpl implements TicketTypeService {

	private final TicketTypeRepository ticketTypeRepository;
	private final EventAccess eventAccess;

	public TicketTypeServiceImpl(TicketTypeRepository ticketTypeRepository, EventAccess eventAccess) {
		this.ticketTypeRepository = ticketTypeRepository;
		this.eventAccess = eventAccess;
	}

	@Override
	@Transactional
	public TicketTypeResponse add(Long eventId, String sellerEmail, TicketTypeRequest request) {
		Event event = eventAccess.requireVisible(eventId, sellerEmail);
		if (!event.ownedBy(sellerEmail)) {
			throw ApiException.forbidden("Event belongs to another seller");
		}
		TicketType ticketType = ticketTypeRepository.save(
				TicketType.create(request.name(), request.price(), request.quota(), event));
		return TicketTypeResponse.from(ticketType);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TicketTypeResponse> findByEventId(Long eventId, String viewerEmail) {
		eventAccess.requireVisible(eventId, viewerEmail);
		return ticketTypeRepository.findByEventId(eventId).stream().map(TicketTypeResponse::from).toList();
	}

}
