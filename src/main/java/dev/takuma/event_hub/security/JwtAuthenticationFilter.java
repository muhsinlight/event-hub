package dev.takuma.event_hub.security;

import dev.takuma.event_hub.entity.AuthSession;
import dev.takuma.event_hub.repository.AuthSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final AuthSessionRepository authSessionRepository;
	private final Clock clock;

	public JwtAuthenticationFilter(JwtService jwtService, AuthSessionRepository authSessionRepository, Clock clock) {
		this.jwtService = jwtService;
		this.authSessionRepository = authSessionRepository;
		this.clock = clock;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = bearer(request);
		if (token == null && request.getHeader("Authorization") == null && !request.getRequestURI().startsWith("/api/")) {
			token = AccessCookie.read(request);
		}
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticate(request, token);
		}
		filterChain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String token) {
		try {
			Claims claims = jwtService.claims(token);
			if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE))) {
				SecurityContextHolder.clearContext();
				return;
			}
			Long sessionId = sessionId(claims);
			String email = claims.getSubject();
			if (sessionId == null || email == null) {
				SecurityContextHolder.clearContext();
				return;
			}
			AuthSession session = authSessionRepository.findByIdWithUser(sessionId).orElse(null);
			Instant now = clock.instant();
			if (session == null || !session.isActive(now) || !email.equals(session.getUser().getEmail())) {
				SecurityContextHolder.clearContext();
				return;
			}
			SecurityUser user = SecurityUser.of(session.getUser(), session.getId());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					user, null, user.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (JwtException | IllegalArgumentException exception) {
			SecurityContextHolder.clearContext();
		}
	}

	private static String bearer(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

	private static Long sessionId(Claims claims) {
		Object value = claims.get(JwtService.CLAIM_SESSION_ID);
		if (value instanceof Integer integer) {
			return integer.longValue();
		}
		if (value instanceof Long sessionId) {
			return sessionId;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return null;
	}

}
