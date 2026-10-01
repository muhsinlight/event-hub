package dev.takuma.event_hub.security;

import dev.takuma.event_hub.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUser implements UserDetails {

	private final User user;
	private final Long sessionId;

	public static SecurityUser of(User user) {
		return new SecurityUser(user, null);
	}

	public static SecurityUser of(User user, Long sessionId) {
		return new SecurityUser(user, sessionId);
	}

	private SecurityUser(User user, Long sessionId) {
		this.user = user;
		this.sessionId = sessionId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	public Long getSessionId() {
		return sessionId;
	}

}
