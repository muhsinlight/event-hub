package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.user.UserResponse;
import dev.takuma.event_hub.entity.Role;
import org.springframework.data.domain.Pageable;

public interface UserService {

	PageResponse<UserResponse> findAll(String adminEmail, Pageable pageable);

	UserResponse assignRole(String adminEmail, Long userId, Role role);

	UserResponse findByEmail(String email);

}
