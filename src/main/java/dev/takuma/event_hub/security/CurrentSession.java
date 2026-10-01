package dev.takuma.event_hub.security;

import dev.takuma.event_hub.utils.ApiException;
import org.springframework.security.core.Authentication;

public final class CurrentSession {

	private CurrentSession() {
	}

	public static Long requireId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)
				|| user.getSessionId() == null) {
			throw ApiException.unauthorized("Unauthorized");
		}
		return user.getSessionId();
	}

}
