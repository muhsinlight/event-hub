package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@NotBlank
	@Column(nullable = false, length = 4)
	private String last4;

	@NotBlank
	@Column(nullable = false, length = 32)
	private String brand;

	@NotNull
	@Column(nullable = false)
	private Instant createdAt;

	@NotNull
	@ManyToOne(optional = false)
	private Order order;

	public Payment() {
	}

	public static Payment paid(Order order, String last4, String brand, Instant createdAt) {
		Payment payment = new Payment();
		payment.order = order;
		payment.amount = order.getAmount();
		payment.last4 = last4;
		payment.brand = brand;
		payment.createdAt = createdAt;
		return payment;
	}

	public Long getId() {
		return id;
	}

}
