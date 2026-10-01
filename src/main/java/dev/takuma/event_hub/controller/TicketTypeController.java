package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.ticket.TicketTypeRequest;
import dev.takuma.event_hub.dto.ticket.TicketTypeResponse;
import dev.takuma.event_hub.service.TicketTypeService;
import dev.takuma.event_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/ticket-types")
public class TicketTypeController {

	private final TicketTypeService ticketTypeService;

	public TicketTypeController(TicketTypeService ticketTypeService) {
		this.ticketTypeService = ticketTypeService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TicketTypeResponse>> add(@PathVariable Long eventId,
			@Valid @RequestBody TicketTypeRequest request, Authentication authentication) {
		return ApiResponse.<TicketTypeResponse>builder()
				.status(HttpStatus.CREATED)
				.message("Ticket type created")
				.data(ticketTypeService.add(eventId, authentication.getName(), request))
				.response();
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TicketTypeResponse>>> findByEvent(@PathVariable Long eventId,
			Authentication authentication) {
		String viewer = authentication == null ? null : authentication.getName();
		return ApiResponse.<List<TicketTypeResponse>>builder()
				.data(ticketTypeService.findByEventId(eventId, viewer))
				.response();
	}

}
