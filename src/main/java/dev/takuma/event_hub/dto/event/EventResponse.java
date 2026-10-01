package dev.takuma.event_hub.dto.event;

import dev.takuma.event_hub.entity.Event;
import java.time.LocalDateTime;

public record EventResponse(Long id, String name, String venue, LocalDateTime startsAt, Event.Status status) {

	public static EventResponse from(Event event) {
		return new EventResponse(event.getId(), event.getName(), event.getVenue(), event.getStartsAt(),
				event.getStatus());
	}

}
