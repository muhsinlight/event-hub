package dev.takuma.event_hub.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class AccessCookie {

	public static final String NAME = "access_token";

	private AccessCookie() {
	}

	public static void write(HttpServletResponse response, String token, Duration maxAge, boolean secure) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, maxAge, secure).toString());
	}

	public static void clear(HttpServletResponse response, boolean secure) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, secure).toString());
	}

	public static String read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
					return cookie.getValue();
				}
			}
		}
		return fromHeader(request.getHeader(HttpHeaders.COOKIE));
	}

	private static String fromHeader(String header) {
		if (header == null) {
			return null;
		}
		for (String part : header.split(";")) {
			String trimmed = part.trim();
			String prefix = NAME + "=";
			if (trimmed.startsWith(prefix)) {
				String value = trimmed.substring(prefix.length());
				return value.isBlank() ? null : value;
			}
		}
		return null;
	}

	private static ResponseCookie cookie(String token, Duration maxAge, boolean secure) {
		return ResponseCookie.from(NAME, token)
				.httpOnly(true)
				.secure(secure)
				.path("/")
				.sameSite("Lax")
				.maxAge(maxAge)
				.build();
	}

}
