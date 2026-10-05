package dev.takuma.event_hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository(@Value("${app.cookie.secure:false}") boolean secure) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.httpOnly(false).secure(secure).sameSite("Lax").path("/"));
		return repository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
			AuthRateLimitFilter authRateLimitFilter, CorsConfigurationSource corsConfigurationSource,
			CsrfTokenRepository csrfTokenRepository) throws Exception {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
						.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
						.ignoringRequestMatchers("/api/**", "/orders", "/orders/**", "/events", "/events/**",
								"/tickets/**", "/logout", "/admin/**"))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.logout(logout -> logout.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/forbidden", "/not-found").permitAll()
						.requestMatchers(HttpMethod.GET, "/login", "/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/login", "/register").permitAll()
						.requestMatchers(HttpMethod.GET, "/", "/favicon.ico").permitAll()
						.requestMatchers(HttpMethod.GET, "/events/new").hasAnyRole("SELLER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/events").hasAnyRole("SELLER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/events/*/ticket-types").hasAnyRole("SELLER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/admin/users").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/admin/users/*/role").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/events/**").permitAll()
						.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").hasRole("ADMIN")
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/tickets/*/qr").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/events", "/api/events/*/ticket-types").hasAnyRole("SELLER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/tickets/*/check-in", "/api/tickets/*/check-in").hasAnyRole("SELLER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/orders", "/orders/*/cancel", "/orders/*/pay", "/api/orders",
								"/api/orders/*/cancel", "/api/orders/*/pay")
						.hasAnyRole("USER", "SELLER", "ADMIN")
						.requestMatchers("/api/users/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/actuator/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> {
							if (isSwaggerUi(request)) {
								response.sendRedirect("/login");
								return;
							}
							if (isHiddenAdmin(request)) {
								hideAdminSurface(request, response);
								return;
							}
							if (isPage(request)) {
								response.sendRedirect("/login");
								return;
							}
							writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
						})
						.accessDeniedHandler((request, response, exception) -> {
							if (isHiddenAdmin(request)) {
								hideAdminSurface(request, response);
								return;
							}
							if (isPage(request)) {
								response.sendRedirect("/forbidden");
								return;
							}
							writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden");
						}))
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	FilterRegistrationBean<AuthRateLimitFilter> authRateLimitRegistration(AuthRateLimitFilter filter) {
		FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	private static boolean isPage(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return !uri.startsWith("/api/") && !uri.startsWith("/actuator/");
	}

	private static boolean isSwaggerUi(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/swagger-ui") || uri.equals("/swagger-ui.html");
	}

	private static boolean isHiddenAdmin(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return isSwaggerUi(request)
				|| uri.startsWith("/v3/api-docs")
				|| uri.startsWith("/admin")
				|| (uri.startsWith("/actuator") && !isPublicHealth(uri));
	}

	private static boolean isPublicHealth(String uri) {
		return "/actuator/health".equals(uri) || uri.startsWith("/actuator/health/");
	}

	private static void hideAdminSurface(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String uri = request.getRequestURI();
		if (uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator")) {
			writeError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
			return;
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		request.getRequestDispatcher("/not-found").forward(request, response);
	}

	private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
	}

	private static final class CsrfCookieFilter extends OncePerRequestFilter {


		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
			CsrfToken csrfToken = resolve(request);
			if (csrfToken != null) {
				csrfToken.getToken();
			}
			filterChain.doFilter(request, response);
		}

		private static CsrfToken resolve(HttpServletRequest request) {
			Object token = request.getAttribute(CsrfToken.class.getName());
			if (token instanceof CsrfToken csrfToken) {
				return csrfToken;
			}
			token = request.getAttribute("_csrf");
			if (token instanceof CsrfToken csrfToken) {
				return csrfToken;
			}
			return null;
		}

	}

}
