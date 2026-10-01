package dev.takuma.event_hub.web;

import dev.takuma.event_hub.dto.event.EventRequest;
import dev.takuma.event_hub.dto.event.EventResponse;
import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.service.EventService;
import dev.takuma.event_hub.service.TicketTypeService;
import dev.takuma.event_hub.utils.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventPageController {

	private final EventService eventService;
	private final TicketTypeService ticketTypeService;

	public EventPageController(EventService eventService, TicketTypeService ticketTypeService) {
		this.eventService = eventService;
		this.ticketTypeService = ticketTypeService;
	}

	@GetMapping("/events/new")
	public String createForm(Model model) {
		model.addAttribute("pageTitle", "New event");
		model.addAttribute("statuses", List.of(Event.Status.DRAFT, Event.Status.PUBLISHED));
		return "event-form";
	}

	@PostMapping("/events")
	public String create(@Valid EventRequest request, BindingResult binding, Authentication authentication,
			RedirectAttributes redirect) {
		String invalid = Pages.redirectIfInvalid(binding, redirect, "/events/new");
		if (invalid != null) {
			return invalid;
		}
		try {
			EventResponse event = eventService.create(authentication.getName(), request);
			return "redirect:/events/" + event.id();
		}
		catch (ApiException exception) {
			Pages.flashApiError(redirect, exception);
			return "redirect:/events/new";
		}
	}

	@GetMapping("/events/{id}")
	public ModelAndView show(@PathVariable Long id) {
		try {
			ModelAndView page = new ModelAndView("event");
			EventResponse event = eventService.findById(id, null);
			page.addObject("pageTitle", event.name());
			page.addObject("event", event);
			page.addObject("ticketTypes", ticketTypeService.findByEventId(id, null));
			return page;
		}
		catch (ApiException exception) {
			return Pages.notFoundUnless(exception, exception.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

}
