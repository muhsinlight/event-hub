package dev.takuma.event_hub.web;

import dev.takuma.event_hub.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ProfilePageController {

	private final UserService userService;

	public ProfilePageController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public ModelAndView show(Authentication authentication) {
		ModelAndView page = new ModelAndView("profile");
		page.addObject("pageTitle", "Profile");
		page.addObject("user", userService.findByEmail(authentication.getName()));
		return page;
	}

}
