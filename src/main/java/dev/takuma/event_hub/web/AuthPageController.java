package dev.takuma.event_hub.web;

import dev.takuma.event_hub.dto.auth.LoginRequest;
import dev.takuma.event_hub.dto.auth.LoginResponse;
import dev.takuma.event_hub.dto.auth.RegisterRequest;
import dev.takuma.event_hub.security.AccessCookie;
import dev.takuma.event_hub.security.CurrentSession;
import dev.takuma.event_hub.security.SecurityUser;
import dev.takuma.event_hub.service.AuthService;
import dev.takuma.event_hub.utils.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthPageController {

	private final AuthService authService;
	private final Duration accessTtl;
	private final boolean secureCookie;

	public AuthPageController(AuthService authService, @Value("${app.jwt.expiration-ms}") long accessTtlMs,
			@Value("${app.cookie.secure:false}") boolean secureCookie) {
		this.authService = authService;
		this.accessTtl = Duration.ofMillis(accessTtlMs);
		this.secureCookie = secureCookie;
	}

	@GetMapping("/login")
	public String loginForm(Model model) {
		model.addAttribute("pageTitle", "Log in");
		return "login";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute LoginRequest loginRequest, BindingResult binding, Model model,
			HttpServletResponse response) {
		model.addAttribute("pageTitle", "Log in");
		if (binding.hasErrors()) {
			model.addAttribute("error", Forms.validationMessage(binding));
			return "login";
		}
		try {
			LoginResponse loggedIn = authService.login(loginRequest);
			AccessCookie.write(response, loggedIn.token(), accessTtl, secureCookie);
			return "redirect:/";
		}
		catch (ApiException exception) {
			Pages.addApiError(model, exception);
			return "login";
		}
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("pageTitle", "Register");
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute RegisterRequest registerRequest, BindingResult binding, Model model,
			RedirectAttributes redirect) {
		if (binding.hasErrors()) {
			model.addAttribute("pageTitle", "Register");
			model.addAttribute("error", Forms.validationMessage(binding));
			return "register";
		}
		try {
			authService.register(registerRequest);
			redirect.addFlashAttribute("notice", "Account created. Log in.");
			return "redirect:/login";
		}
		catch (ApiException exception) {
			model.addAttribute("pageTitle", "Register");
			Pages.addApiError(model, exception);
			return "register";
		}
	}

	@PostMapping("/logout")
	public String logout(Authentication authentication, HttpServletResponse response) {
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
			authService.logout(CurrentSession.requireId(authentication));
		}
		AccessCookie.clear(response, secureCookie);
		return "redirect:/";
	}

}
