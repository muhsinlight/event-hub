package dev.takuma.event_hub.web;

import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPageController {

	private final UserService userService;

	public AdminPageController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/admin/users")
	public String users(
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
			Authentication authentication, Model model) {
		model.addAttribute("pageTitle", "People");
		model.addAttribute("users", userService.findAll(authentication.getName(), pageable));
		return "users";
	}

	@PostMapping("/admin/users/{id}/role")
	public String assignRole(@PathVariable Long id, @RequestParam Role role, Authentication authentication,
			RedirectAttributes redirect) {
		Pages.runOrFlash(redirect, () -> userService.assignRole(authentication.getName(), id, role));
		return "redirect:/admin/users";
	}

}
