package dev.takuma.event_hub.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.TestData;
import dev.takuma.event_hub.dto.event.EventRequest;
import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.repository.EventRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.service.support.EventAccess;
import dev.takuma.event_hub.utils.ApiException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

	private static final Long EVENT_ID = 5L;

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventAccess eventAccess;

	@Mock
	private EventRequest request;

	@Captor
	private ArgumentCaptor<Event> savedEvent;

	@InjectMocks
	private EventServiceImpl eventService;

	@Test
	void createAssignsAuthenticatedSellerAsOwner() {
		when(userRepository.findByEmail(TestData.SELLER_EMAIL)).thenReturn(Optional.of(TestData.seller()));
		when(request.name()).thenReturn("Concert");
		when(request.venue()).thenReturn("Arena");
		when(request.startsAt()).thenReturn(LocalDateTime.of(2030, 1, 1, 20, 0));
		when(request.status()).thenReturn(Event.Status.DRAFT);
		when(eventRepository.save(any(Event.class))).thenAnswer(returnsFirstArg());

		eventService.create(TestData.SELLER_EMAIL, request);

		verify(eventRepository).save(savedEvent.capture());
		assertThat(savedEvent.getValue().ownedBy(TestData.SELLER_EMAIL)).isTrue();
	}

	@Test
	void draftIsHiddenFromAnonymousAndOtherSellers() {
		when(eventAccess.requireVisible(EVENT_ID, null)).thenThrow(ApiException.notFound("Event not found"));
		when(eventAccess.requireVisible(EVENT_ID, TestData.OTHER_SELLER_EMAIL))
				.thenThrow(ApiException.notFound("Event not found"));

		assertThatThrownBy(() -> eventService.findById(EVENT_ID, null))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
		assertThatThrownBy(() -> eventService.findById(EVENT_ID, TestData.OTHER_SELLER_EMAIL))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void draftIsVisibleToItsSeller() {
		when(eventAccess.requireVisible(EVENT_ID, TestData.SELLER_EMAIL)).thenReturn(TestData.draftEvent());

		assertThat(eventService.findById(EVENT_ID, TestData.SELLER_EMAIL).status()).isEqualTo(Event.Status.DRAFT);
	}

}
