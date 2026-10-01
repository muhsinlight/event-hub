package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.auth.LoginRequest;
import dev.takuma.event_hub.dto.auth.LoginResponse;
import dev.takuma.event_hub.dto.auth.RegisterRequest;
import dev.takuma.event_hub.dto.user.UserResponse;
import dev.takuma.event_hub.entity.AuthSession;
import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.repository.AuthSessionRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.security.JwtRefreshProperties;
import dev.takuma.event_hub.security.JwtService;
import dev.takuma.event_hub.security.RefreshTokens;
import dev.takuma.event_hub.service.AuthService;
import dev.takuma.event_hub.utils.ApiException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final AuthSessionRepository authSessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final Clock clock;
	private final JwtRefreshProperties refreshProperties;

	public AuthServiceImpl(UserRepository userRepository, AuthSessionRepository authSessionRepository,
			PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService,
			Clock clock, JwtRefreshProperties refreshProperties) {
		this.userRepository = userRepository;
		this.authSessionRepository = authSessionRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.clock = clock;
		this.refreshProperties = refreshProperties;
	}

	@Override
	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw ApiException.conflict("Email already used");
		}
		User user = userRepository.save(User.create(request.name(), request.email(),
				passwordEncoder.encode(request.password()), Role.USER));
		return UserResponse.from(user);
	}

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		}
		catch (AuthenticationException exception) {
			throw ApiException.unauthorized("Invalid email or password");
		}
		User user = ApiException.orNotFound(userRepository.findByEmail(request.email()), "User not found");
		return issueTokens(user);
	}

	@Override
	@Transactional
	public LoginResponse refresh(String refreshToken) {
		AuthSession session = authSessionRepository.findByRefreshTokenHash(RefreshTokens.hash(refreshToken)).orElse(null);
		Instant now = clock.instant();
		if (session == null || !session.isActive(now)) {
			throw ApiException.unauthorized("Invalid refresh token");
		}
		String rawRefresh = RefreshTokens.generate();
		session.rotateRefresh(RefreshTokens.hash(rawRefresh), now.plusMillis(refreshProperties.expirationMs()));
		authSessionRepository.save(session);
		return new LoginResponse(jwtService.generateAccess(session.getUser().getEmail(), session.getId()), rawRefresh,
				UserResponse.from(session.getUser()));
	}

	@Override
	@Transactional
	public void logout(Long sessionId) {
		AuthSession session = ApiException.orNotFound(authSessionRepository.findById(sessionId), "Session not found");
		if (!session.isRevoked()) {
			session.revoke(clock.instant());
			authSessionRepository.save(session);
		}
	}

	private LoginResponse issueTokens(User user) {
		String rawRefresh = RefreshTokens.generate();
		Instant expiresAt = clock.instant().plusMillis(refreshProperties.expirationMs());
		AuthSession session = authSessionRepository.save(AuthSession.create(user, RefreshTokens.hash(rawRefresh), expiresAt));
		return new LoginResponse(jwtService.generateAccess(user.getEmail(), session.getId()), rawRefresh,
				UserResponse.from(user));
	}

}
