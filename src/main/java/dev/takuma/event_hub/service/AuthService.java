package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.auth.LoginRequest;
import dev.takuma.event_hub.dto.auth.LoginResponse;
import dev.takuma.event_hub.dto.auth.RegisterRequest;
import dev.takuma.event_hub.dto.user.UserResponse;

public interface AuthService {

	UserResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	LoginResponse refresh(String refreshToken);

	void logout(Long sessionId);

}
