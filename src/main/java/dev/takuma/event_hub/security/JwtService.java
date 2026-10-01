package dev.takuma.event_hub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	public static final String CLAIM_SESSION_ID = "sid";
	public static final String CLAIM_TYPE = "typ";
	public static final String TYPE_ACCESS = "access";

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET is required");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateAccess(String email, Long sessionId) {
		Date now = new Date();
		return Jwts.builder()
				.subject(email)
				.claim(CLAIM_SESSION_ID, sessionId)
				.claim(CLAIM_TYPE, TYPE_ACCESS)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	public Claims claims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}
