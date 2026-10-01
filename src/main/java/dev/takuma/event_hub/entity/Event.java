package dev.takuma.event_hub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@NotBlank
	@Column(nullable = false)
	private String venue;

	@NotNull
	@Column(nullable = false)
	private LocalDateTime startsAt;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

	@ManyToOne(fetch = FetchType.LAZY)
	private User seller;

	public enum Status {
		DRAFT, PUBLISHED, CANCELLED
	}

	public Event() {
	}

	public static Event create(String name, String venue, LocalDateTime startsAt, Status status, User seller) {
		Event event = new Event();
		event.name = name;
		event.venue = venue;
		event.startsAt = startsAt;
		event.status = status;
		event.seller = seller;
		return event;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getVenue() {
		return venue;
	}

	public LocalDateTime getStartsAt() {
		return startsAt;
	}

	public Status getStatus() {
		return status;
	}

	public boolean isPublished() {
		return status == Status.PUBLISHED;
	}

	public boolean ownedBy(String email) {
		return email != null && seller != null && email.equals(seller.getEmail());
	}

	public boolean isVisibleTo(String email) {
		return status != Status.DRAFT || ownedBy(email);
	}

}
