package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.service.TicketService;
import dev.takuma.event_hub.utils.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService ticketService;

	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping("/{code}")
	public ResponseEntity<ApiResponse<TicketResponse>> findByCode(@PathVariable String code,
			Authentication authentication) {
		return ApiResponse.<TicketResponse>builder()
				.data(ticketService.findByCode(code, authentication.getName()))
				.response();
	}

	@GetMapping(value = "/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public byte[] qr(@PathVariable String code) {
		return ticketService.qr(code);
	}

	@PostMapping("/{code}/check-in")
	public ResponseEntity<ApiResponse<TicketResponse>> checkIn(@PathVariable String code,
			Authentication authentication) {
		return ApiResponse.<TicketResponse>builder()
				.message("Checked in")
				.data(ticketService.checkIn(code, authentication.getName()))
				.response();
	}

}
