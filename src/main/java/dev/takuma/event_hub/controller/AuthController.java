package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.auth.LoginRequest;
import dev.takuma.event_hub.dto.auth.LoginResponse;
import dev.takuma.event_hub.dto.auth.RefreshTokenRequest;
import dev.takuma.event_hub.dto.auth.RegisterRequest;
import dev.takuma.event_hub.dto.user.UserResponse;
import dev.takuma.event_hub.security.CurrentSession;
import dev.takuma.event_hub.service.AuthService;
import dev.takuma.event_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.<UserResponse>builder()
				.status(HttpStatus.CREATED)
				.message("User created")
				.data(authService.register(request))
				.response();
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.<LoginResponse>builder()
				.message("Logged in")
				.data(authService.login(request))
				.response();
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.<LoginResponse>builder()
				.message("Token refreshed")
				.data(authService.refresh(request.refreshToken()))
				.response();
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
		authService.logout(CurrentSession.requireId(authentication));
		return ApiResponse.<Void>builder()
				.message("Logged out")
				.response();
	}

}
