package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.event.EventRequest;
import dev.takuma.event_hub.dto.event.EventResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.repository.EventRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.service.EventService;
import dev.takuma.event_hub.service.support.EventAccess;
import dev.takuma.event_hub.utils.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventServiceImpl implements EventService {

	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final EventAccess eventAccess;

	public EventServiceImpl(EventRepository eventRepository, UserRepository userRepository, EventAccess eventAccess) {
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.eventAccess = eventAccess;
	}

	@Override
	@Transactional
	public EventResponse create(String sellerEmail, EventRequest request) {
		User seller = ApiException.orNotFound(userRepository.findByEmail(sellerEmail), "User not found");
		Event event = eventRepository.save(
				Event.create(request.name(), request.venue(), request.startsAt(), request.status(), seller));
		return EventResponse.from(event);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EventResponse> findAll(String viewerEmail, Pageable pageable) {
		return PageResponse.from(eventRepository.findVisible(viewerEmail, pageable), EventResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public EventResponse findById(Long id, String viewerEmail) {
		return EventResponse.from(eventAccess.requireVisible(id, viewerEmail));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EventResponse> findByName(String name, String viewerEmail, Pageable pageable) {
		return PageResponse.from(eventRepository.findVisibleByName(name, viewerEmail, pageable), EventResponse::from);
	}

}
