package dev.takuma.event_hub.dto.auth;

import dev.takuma.event_hub.dto.user.UserResponse;

public record LoginResponse(String token, String refreshToken, UserResponse user) {
}
