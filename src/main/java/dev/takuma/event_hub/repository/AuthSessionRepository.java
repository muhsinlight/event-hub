package dev.takuma.event_hub.repository;

import dev.takuma.event_hub.entity.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

	@Query("select session from AuthSession session join fetch session.user where session.refreshTokenHash = :hash")
	Optional<AuthSession> findByRefreshTokenHash(@Param("hash") String hash);

	@Query("select session from AuthSession session join fetch session.user where session.id = :id")
	Optional<AuthSession> findByIdWithUser(@Param("id") Long id);

}
