package dev.takuma.event_hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final int MAX_ATTEMPTS = 10;
	private static final Duration WINDOW = Duration.ofMinutes(1);
	private static final int MAX_TRACKED_CLIENTS = 10_000;
	private static final Set<String> LIMITED = Set.of("/login", "/register", "/api/auth/login", "/api/auth/register");

	private final boolean enabled;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public AuthRateLimitFilter(@Value("${app.auth.rate-limit-enabled:true}") boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !enabled || !"POST".equalsIgnoreCase(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String key = request.getRemoteAddr() + " " + request.getRequestURI();
		if (!allow(key)) {
			reject(request, response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean allow(String key) {
		long now = System.currentTimeMillis();
		Window window = windows.compute(key, (ignored, current) -> {
			if (current == null || current.hasElapsed(now)) {
				return new Window(now);
			}
			return current;
		});
		if (windows.size() > MAX_TRACKED_CLIENTS) {
			windows.values().removeIf(stale -> stale.hasElapsed(now));
		}
		return window.attempts.incrementAndGet() <= MAX_ATTEMPTS;
	}

	private static void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (request.getRequestURI().startsWith("/api/")) {
			response.setStatus(429);
			response.setContentType("application/json");
			response.getWriter().write("{\"status\":429,\"message\":\"Too many attempts\",\"data\":null}");
			return;
		}
		String page = "/register".equals(request.getRequestURI()) ? "/register" : "/login";
		response.sendRedirect(page + "?limited");
	}

	private static final class Window {

		private final long startedAt;
		private final AtomicInteger attempts = new AtomicInteger();

		private Window(long startedAt) {
			this.startedAt = startedAt;
		}

		private boolean hasElapsed(long now) {
			return now - startedAt >= WINDOW.toMillis();
		}

	}

}
