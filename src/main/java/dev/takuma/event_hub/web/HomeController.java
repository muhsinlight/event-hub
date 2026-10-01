package dev.takuma.event_hub.web;

import dev.takuma.event_hub.service.EventService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	private final EventService eventService;

	public HomeController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping("/")
	public String home(
			@PageableDefault(size = 12, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable,
			Model model) {
		model.addAttribute("pageTitle", "Event Hub");
		model.addAttribute("events", eventService.findAll(null, pageable));
		return "index";
	}

}
