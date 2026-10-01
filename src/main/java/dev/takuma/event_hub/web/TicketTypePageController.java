package dev.takuma.event_hub.web;

import dev.takuma.event_hub.dto.ticket.TicketTypeRequest;
import dev.takuma.event_hub.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TicketTypePageController {

	private final TicketTypeService ticketTypeService;

	public TicketTypePageController(TicketTypeService ticketTypeService) {
		this.ticketTypeService = ticketTypeService;
	}

	@PostMapping("/events/{eventId}/ticket-types")
	public String add(@PathVariable Long eventId, @Valid TicketTypeRequest request, BindingResult binding,
			Authentication authentication, RedirectAttributes redirect) {
		String eventPath = "/events/" + eventId;
		String invalid = Pages.redirectIfInvalid(binding, redirect, eventPath);
		if (invalid != null) {
			return invalid;
		}
		Pages.runOrFlash(redirect, () -> ticketTypeService.add(eventId, authentication.getName(), request));
		return "redirect:" + eventPath;
	}

}
