package dev.takuma.event_hub.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorPageController {

	@GetMapping("/forbidden")
	public ModelAndView forbidden() {
		ModelAndView page = new ModelAndView("forbidden", HttpStatus.FORBIDDEN);
		page.addObject("pageTitle", "Forbidden");
		return page;
	}

	@GetMapping("/not-found")
	public ModelAndView notFound() {
		ModelAndView page = new ModelAndView("not-found", HttpStatus.NOT_FOUND);
		page.addObject("pageTitle", "Not found");
		page.addObject("message", "Page not found");
		return page;
	}

}
