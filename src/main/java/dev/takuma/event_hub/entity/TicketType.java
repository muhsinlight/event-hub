package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
public class TicketType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@NotNull
	@Positive
	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Positive
	@Column(nullable = false)
	private int quota;

	@Min(0)
	@Column(nullable = false)
	private int soldCount;

	@ManyToOne(optional = false)
	private Event event;

	public TicketType() {
	}

	public static TicketType create(String name, BigDecimal price, int quota, Event event) {
		TicketType ticketType = new TicketType();
		ticketType.name = name;
		ticketType.price = price;
		ticketType.quota = quota;
		ticketType.soldCount = 0;
		ticketType.event = event;
		return ticketType;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getQuota() {
		return quota;
	}

	public int getSoldCount() {
		return soldCount;
	}

	public Event getEvent() {
		return event;
	}

	public boolean hasCapacity(int quantity) {
		return (long) soldCount + quantity <= quota;
	}

	public void increaseSold(int quantity) {
		this.soldCount += quantity;
	}

	public void decreaseSold() {
		releaseSold(1);
	}

	public void releaseSold(int quantity) {
		if (quantity < 0 || (long) soldCount - quantity < 0) {
			throw new IllegalStateException("Sold count cannot go below zero");
		}
		this.soldCount -= quantity;
	}

}
