package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.user.RoleAssignmentRequest;
import dev.takuma.event_hub.dto.user.UserResponse;
import dev.takuma.event_hub.service.UserService;
import dev.takuma.event_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> findAll(
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
			Authentication authentication) {
		return ApiResponse.<PageResponse<UserResponse>>builder()
				.data(userService.findAll(authentication.getName(), pageable))
				.response();
	}

	@PostMapping("/{id}/role")
	public ResponseEntity<ApiResponse<UserResponse>> assignRole(@PathVariable Long id,
			@Valid @RequestBody RoleAssignmentRequest request, Authentication authentication) {
		return ApiResponse.<UserResponse>builder()
				.message("Role updated")
				.data(userService.assignRole(authentication.getName(), id, request.role()))
				.response();
	}

}
