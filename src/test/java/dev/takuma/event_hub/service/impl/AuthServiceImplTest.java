package dev.takuma.event_hub.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.TestData;
import dev.takuma.event_hub.entity.AuthSession;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.dto.auth.LoginRequest;
import dev.takuma.event_hub.dto.auth.LoginResponse;
import dev.takuma.event_hub.dto.auth.RegisterRequest;
import dev.takuma.event_hub.repository.AuthSessionRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.security.JwtRefreshProperties;
import dev.takuma.event_hub.security.JwtService;
import dev.takuma.event_hub.utils.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuthSessionRepository authSessionRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@Mock
	private Clock clock;

	@Mock
	private JwtRefreshProperties refreshProperties;

	@Mock
	private RegisterRequest registerRequest;

	@Mock
	private LoginRequest loginRequest;

	@InjectMocks
	private AuthServiceImpl authService;

	@Test
	void registerRejectsDuplicateEmail() {
		when(registerRequest.email()).thenReturn(TestData.BUYER_EMAIL);
		when(userRepository.existsByEmail(TestData.BUYER_EMAIL)).thenReturn(true);

		assertThatThrownBy(() -> authService.register(registerRequest))
				.isInstanceOf(ApiException.class)
				.hasMessage("Email already used")
				.extracting("status").isEqualTo(HttpStatus.CONFLICT);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void loginWithWrongPasswordIsUnauthorized() {
		when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(BadCredentialsException.class);

		assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(ApiException.class)
				.hasMessage("Invalid email or password")
				.extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anyAuthenticationFailureGetsTheSameAnswer() {
		when(authenticationManager.authenticate(any(Authentication.class)))
				.thenThrow(InternalAuthenticationServiceException.class);

		assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(ApiException.class)
				.hasMessage("Invalid email or password")
				.extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void loginReturnsTokenForAuthenticatedUser() {
		when(loginRequest.email()).thenReturn(TestData.BUYER_EMAIL);
		when(userRepository.findByEmail(TestData.BUYER_EMAIL)).thenReturn(Optional.of(TestData.buyer()));
		when(clock.instant()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));
		when(refreshProperties.expirationMs()).thenReturn(604_800_000L);
		when(authSessionRepository.save(any(AuthSession.class))).thenAnswer(returnsFirstArg());
		when(jwtService.generateAccess(eq(TestData.BUYER_EMAIL), nullable(Long.class))).thenReturn("jwt-token");

		LoginResponse response = authService.login(loginRequest);

		assertThat(response.token()).isEqualTo("jwt-token");
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.user().email()).isEqualTo(TestData.BUYER_EMAIL);
		verify(authSessionRepository).save(any(AuthSession.class));
	}

}
