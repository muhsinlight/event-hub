package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@NotBlank
	@Column(nullable = false, unique = true, length = 64)
	private String refreshTokenHash;

	@NotNull
	@Column(nullable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	public AuthSession() {
	}

	public static AuthSession create(User user, String refreshTokenHash, Instant expiresAt) {
		AuthSession session = new AuthSession();
		session.user = user;
		session.refreshTokenHash = refreshTokenHash;
		session.expiresAt = expiresAt;
		return session;
	}

	public void rotateRefresh(String refreshTokenHash, Instant expiresAt) {
		this.refreshTokenHash = refreshTokenHash;
		this.expiresAt = expiresAt;
	}

	public void revoke(Instant at) {
		this.revokedAt = at;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isActive(Instant now) {
		return !isRevoked() && expiresAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

}
