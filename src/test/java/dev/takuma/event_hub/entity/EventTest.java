package dev.takuma.event_hub.entity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.takuma.event_hub.TestData;
import org.junit.jupiter.api.Test;

class EventTest {

	@Test
	void draftIsVisibleOnlyToItsSeller() {
		Event draft = TestData.draftEvent();

		assertThat(draft.isVisibleTo(TestData.SELLER_EMAIL)).isTrue();
		assertThat(draft.isVisibleTo(TestData.OTHER_SELLER_EMAIL)).isFalse();
		assertThat(draft.isVisibleTo(null)).isFalse();
	}

	@Test
	void publishedAndCancelledAreVisibleToEveryone() {
		assertThat(TestData.publishedEvent().isVisibleTo(null)).isTrue();
		assertThat(TestData.event(Event.Status.CANCELLED).isVisibleTo(null)).isTrue();
	}

	@Test
	void anonymousNeverOwnsAnEvent() {
		assertThat(TestData.publishedEvent().ownedBy(null)).isFalse();
	}

}
