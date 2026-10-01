package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.event.EventRequest;
import dev.takuma.event_hub.dto.event.EventResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.service.EventService;
import dev.takuma.event_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody EventRequest request,
			Authentication authentication) {
		return ApiResponse.<EventResponse>builder()
				.status(HttpStatus.CREATED)
				.message("Event created")
				.data(eventService.create(authentication.getName(), request))
				.response();
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> findAll(
			@PageableDefault(size = 12, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable,
			Authentication authentication) {
		return ApiResponse.<PageResponse<EventResponse>>builder()
				.data(eventService.findAll(viewer(authentication), pageable))
				.response();
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> search(@RequestParam String name,
			@PageableDefault(size = 12, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable,
			Authentication authentication) {
		return ApiResponse.<PageResponse<EventResponse>>builder()
				.data(eventService.findByName(name, viewer(authentication), pageable))
				.response();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<EventResponse>> findById(@PathVariable Long id, Authentication authentication) {
		return ApiResponse.<EventResponse>builder()
				.data(eventService.findById(id, viewer(authentication)))
				.response();
	}

	private static String viewer(Authentication authentication) {
		return authentication == null ? null : authentication.getName();
	}

}
