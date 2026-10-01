package dev.takuma.event_hub.repository;

import dev.takuma.event_hub.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
