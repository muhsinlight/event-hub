package dev.takuma.event_hub.web;

import dev.takuma.event_hub.service.TicketService;
import dev.takuma.event_hub.utils.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TicketPageController {

	private final TicketService ticketService;

	public TicketPageController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping("/tickets")
	public ModelAndView lookup(@RequestParam(required = false) String code) {
		if (code == null || code.isBlank()) {
			ModelAndView page = new ModelAndView("ticket-lookup");
			page.addObject("pageTitle", "Tickets");
			return page;
		}
		return new ModelAndView("redirect:/tickets/" + code.trim());
	}

	@GetMapping("/tickets/{code}")
	public ModelAndView show(@PathVariable String code, Authentication authentication) {
		try {
			ModelAndView page = new ModelAndView("ticket");
			page.addObject("pageTitle", "Ticket");
			page.addObject("ticket", ticketService.findByCode(code, authentication.getName()));
			return page;
		}
		catch (ApiException exception) {
			return Pages.notFoundUnless(exception, "Ticket not found", HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
		}
	}

	@PostMapping("/tickets/{code}/check-in")
	public String checkIn(@PathVariable String code, Authentication authentication, RedirectAttributes redirect) {
		Pages.runOrFlash(redirect, () -> ticketService.checkIn(code, authentication.getName()));
		return "redirect:/tickets/" + code;
	}

}
