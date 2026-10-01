package dev.takuma.event_hub.dto.user;

import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.entity.User;

public record UserResponse(Long id, String name, String email, Role role) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}

}
