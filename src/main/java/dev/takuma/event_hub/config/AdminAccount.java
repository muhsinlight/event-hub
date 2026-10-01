package dev.takuma.event_hub.config;

import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class AdminAccount {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public AdminAccount(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void ensure() {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}
		userRepository.findByEmail(email).ifPresentOrElse(user -> {
			if (user.getRole() != Role.ADMIN) {
				user.changeRole(Role.ADMIN);
			}
		}, () -> userRepository.save(User.create("Admin", email, passwordEncoder.encode(password), Role.ADMIN)));
	}

}
