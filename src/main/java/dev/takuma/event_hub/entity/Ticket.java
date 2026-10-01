package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false, unique = true)
	private String code;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

	@NotNull
	@ManyToOne(optional = false)
	private Order order;

	@NotNull
	@ManyToOne(optional = false)
	private TicketType ticketType;

	@Transient
	private String qr;

	public enum Status {
		ISSUED, CHECKED_IN, CANCELLED
	}

	public Ticket() {
	}

	public static Ticket issued(Order order, TicketType ticketType) {
		Ticket ticket = new Ticket();
		ticket.code = UUID.randomUUID().toString();
		ticket.status = Status.ISSUED;
		ticket.order = order;
		ticket.ticketType = ticketType;
		return ticket;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public Status getStatus() {
		return status;
	}

	public Order getOrder() {
		return order;
	}

	public TicketType getTicketType() {
		return ticketType;
	}

	public String getQr() {
		return qr;
	}

	public boolean isIssued() {
		return status == Status.ISSUED;
	}

	public boolean isCheckedIn() {
		return status == Status.CHECKED_IN;
	}

	public void markCancelled() {
		this.status = Status.CANCELLED;
	}

	public void markCheckedIn() {
		this.status = Status.CHECKED_IN;
	}

	public void attachQr(String qr) {
		this.qr = qr;
	}

	public boolean checkableBy(String email) {
		return ticketType.getEvent().ownedBy(email);
	}

	public boolean visibleTo(String email) {
		return order.ownedBy(email) || checkableBy(email);
	}

}
