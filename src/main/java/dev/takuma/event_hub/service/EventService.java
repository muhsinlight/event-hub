package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.event.EventRequest;
import dev.takuma.event_hub.dto.event.EventResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EventService {

	EventResponse create(String sellerEmail, EventRequest request);

	PageResponse<EventResponse> findAll(String viewerEmail, Pageable pageable);

	EventResponse findById(Long id, String viewerEmail);

	PageResponse<EventResponse> findByName(String name, String viewerEmail, Pageable pageable);

}
