package dev.takuma.event_hub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtRefreshProperties {

	private final long expirationMs;

	public JwtRefreshProperties(@Value("${app.jwt.refresh-expiration-ms}") long expirationMs) {
		this.expirationMs = expirationMs;
	}

	public long expirationMs() {
		return expirationMs;
	}

}
