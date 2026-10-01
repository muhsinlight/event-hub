package dev.takuma.event_hub.service.support;

import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.repository.EventRepository;
import dev.takuma.event_hub.utils.ApiException;
import org.springframework.stereotype.Component;

@Component
public class EventAccess {

	private final EventRepository eventRepository;

	public EventAccess(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public Event requireVisible(Long eventId, String viewerEmail) {
		return ApiException.orNotFound(
				eventRepository.findById(eventId).filter(event -> event.isVisibleTo(viewerEmail)), "Event not found");
	}

}
