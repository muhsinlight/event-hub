package dev.takuma.event_hub.entity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.takuma.event_hub.TestData;
import org.junit.jupiter.api.Test;

class TicketTypeTest {

	@Test
	void createStartsWithNothingSold() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);

		assertThat(ticketType.getSoldCount()).isZero();
	}

	@Test
	void hasCapacityUpToQuotaInclusive() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		ticketType.increaseSold(7);

		assertThat(ticketType.hasCapacity(3)).isTrue();
		assertThat(ticketType.hasCapacity(4)).isFalse();
	}

	@Test
	void hasCapacityDoesNotOverflow() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), Integer.MAX_VALUE);
		ticketType.increaseSold(Integer.MAX_VALUE - 1);

		assertThat(ticketType.hasCapacity(Integer.MAX_VALUE)).isFalse();
	}

}
