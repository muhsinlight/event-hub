package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.user.UserResponse;
import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.service.UserService;
import dev.takuma.event_hub.utils.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<UserResponse> findAll(String adminEmail, Pageable pageable) {
		requireAdmin(adminEmail);
		return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
	}

	@Override
	@Transactional
	public UserResponse assignRole(String adminEmail, Long userId, Role role) {
		requireAdmin(adminEmail);
		if (role != Role.USER && role != Role.SELLER) {
			throw ApiException.forbidden("Only user or seller can be assigned");
		}
		User user = ApiException.orNotFound(userRepository.findById(userId), "User not found");
		if (user.getRole() == Role.ADMIN) {
			throw ApiException.forbidden("Admin role cannot be changed");
		}
		user.changeRole(role);
		return UserResponse.from(user);
	}

	private User requireAdmin(String email) {
		User admin = ApiException.orNotFound(userRepository.findByEmail(email), "User not found");
		if (admin.getRole() != Role.ADMIN) {
			throw ApiException.forbidden("Admin only");
		}
		return admin;
	}

}
