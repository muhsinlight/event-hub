package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String buyerName;

	@NotBlank
	@Email
	@Column(nullable = false)
	private String buyerEmail;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

	@ManyToOne(optional = false)
	private User user;

	@ManyToOne
	private TicketType ticketType;

	@Column(nullable = false)
	private int quantity;

	@NotNull
	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	public enum Status {
		AWAITING_PAYMENT, CONFIRMED, CANCELLED
	}

	public Order() {
	}

	public static Order awaitingPayment(User buyer, TicketType ticketType, int quantity) {
		Order order = new Order();
		order.buyerName = buyer.getName();
		order.buyerEmail = buyer.getEmail();
		order.user = buyer;
		order.ticketType = ticketType;
		order.quantity = quantity;
		order.amount = ticketType.getPrice().multiply(BigDecimal.valueOf(quantity));
		order.status = Status.AWAITING_PAYMENT;
		return order;
	}

	public static Order confirmed(User buyer) {
		Order order = new Order();
		order.buyerName = buyer.getName();
		order.buyerEmail = buyer.getEmail();
		order.user = buyer;
		order.quantity = 0;
		order.amount = BigDecimal.ZERO;
		order.status = Status.CONFIRMED;
		return order;
	}

	public Long getId() {
		return id;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public String getBuyerEmail() {
		return buyerEmail;
	}

	public Status getStatus() {
		return status;
	}

	public User getUser() {
		return user;
	}

	public boolean ownedBy(String email) {
		if (email == null) {
			return false;
		}
		if (user != null) {
			return email.equals(user.getEmail());
		}
		return email.equals(buyerEmail);
	}

	public TicketType getTicketType() {
		return ticketType;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public boolean isCancelled() {
		return status == Status.CANCELLED;
	}

	public boolean isAwaitingPayment() {
		return status == Status.AWAITING_PAYMENT;
	}

	public void markPaid() {
		this.status = Status.CONFIRMED;
	}

	public void markCancelled() {
		this.status = Status.CANCELLED;
	}

}
