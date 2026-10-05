package dev.takuma.event_hub.web;

import dev.takuma.event_hub.security.SecurityUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "dev.takuma.event_hub.web")
public class PageModelAdvice {

	@ModelAttribute("account")
	public PageAccount account(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		boolean seller = admin || authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SELLER".equals(authority.getAuthority()));
		String name = authentication.getPrincipal() instanceof SecurityUser user ? user.getDisplayName()
				: authentication.getName();
		return new PageAccount(name, seller, admin);
	}

}
