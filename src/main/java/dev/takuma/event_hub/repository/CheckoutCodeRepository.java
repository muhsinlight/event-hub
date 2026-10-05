package dev.takuma.event_hub.repository;

import dev.takuma.event_hub.entity.CheckoutCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutCodeRepository extends JpaRepository<CheckoutCode, Long> {

	boolean existsByCodeIgnoreCase(String code);

}
