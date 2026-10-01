package dev.takuma.event_hub.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.TestData;
import dev.takuma.event_hub.entity.TicketType;
import dev.takuma.event_hub.dto.ticket.TicketTypeRequest;
import dev.takuma.event_hub.dto.ticket.TicketTypeResponse;
import dev.takuma.event_hub.repository.TicketTypeRepository;
import dev.takuma.event_hub.service.support.EventAccess;
import dev.takuma.event_hub.utils.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

	private static final Long EVENT_ID = 3L;

	@Mock
	private TicketTypeRepository ticketTypeRepository;

	@Mock
	private EventAccess eventAccess;

	@Mock
	private TicketTypeRequest request;

	@InjectMocks
	private TicketTypeServiceImpl ticketTypeService;

	@Test
	void ownerAddsTicketTypeWithNothingSold() {
		when(eventAccess.requireVisible(EVENT_ID, TestData.SELLER_EMAIL)).thenReturn(TestData.publishedEvent());
		when(request.name()).thenReturn("VIP");
		when(request.price()).thenReturn(BigDecimal.TEN);
		when(request.quota()).thenReturn(50);
		when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(returnsFirstArg());

		TicketTypeResponse response = ticketTypeService.add(EVENT_ID, TestData.SELLER_EMAIL, request);

		assertThat(response.quota()).isEqualTo(50);
		assertThat(response.soldCount()).isZero();
	}

	@Test
	void anotherSellerCannotAddTicketType() {
		when(eventAccess.requireVisible(EVENT_ID, TestData.OTHER_SELLER_EMAIL)).thenReturn(TestData.publishedEvent());

		assertThatThrownBy(() -> ticketTypeService.add(EVENT_ID, TestData.OTHER_SELLER_EMAIL, request))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
		verify(ticketTypeRepository, never()).save(any(TicketType.class));
	}

	@Test
	void anotherSellersDraftLooksMissing() {
		when(eventAccess.requireVisible(EVENT_ID, TestData.OTHER_SELLER_EMAIL))
				.thenThrow(ApiException.notFound("Event not found"));

		assertThatThrownBy(() -> ticketTypeService.findByEventId(EVENT_ID, TestData.OTHER_SELLER_EMAIL))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
	}

}
