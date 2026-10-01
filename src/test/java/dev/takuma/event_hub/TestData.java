package dev.takuma.event_hub;

import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.entity.Order;
import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.entity.Ticket;
import dev.takuma.event_hub.entity.TicketType;
import dev.takuma.event_hub.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestData {

	public static final String BUYER_EMAIL = "buyer@test.dev";
	public static final String SELLER_EMAIL = "seller@test.dev";
	public static final String OTHER_SELLER_EMAIL = "other-seller@test.dev";
	public static final String STRANGER_EMAIL = "stranger@test.dev";

	private TestData() {
	}

	public static User buyer() {
		return User.create("Buyer", BUYER_EMAIL, "encoded", Role.USER);
	}

	public static User seller() {
		return User.create("Seller", SELLER_EMAIL, "encoded", Role.SELLER);
	}

	public static Event publishedEvent() {
		return event(Event.Status.PUBLISHED);
	}

	public static Event draftEvent() {
		return event(Event.Status.DRAFT);
	}

	public static Event event(Event.Status status) {
		return Event.create("Concert", "Arena", LocalDateTime.of(2030, 1, 1, 20, 0), status, seller());
	}

	public static TicketType ticketType(Event event, int quota) {
		return TicketType.create("VIP", BigDecimal.TEN, quota, event);
	}

	public static Ticket issuedTicket(Order order, TicketType ticketType) {
		return Ticket.issued(order, ticketType);
	}

}
