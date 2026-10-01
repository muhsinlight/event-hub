package dev.takuma.event_hub.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.TestData;
import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.entity.Order;
import dev.takuma.event_hub.entity.Ticket;
import dev.takuma.event_hub.repository.TicketRepository;
import dev.takuma.event_hub.service.QrService;
import dev.takuma.event_hub.utils.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

	private static final String CODE = "ticket-code";

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private QrService qrService;

	@InjectMocks
	private TicketServiceImpl ticketService;

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		ticket = TestData.issuedTicket(Order.confirmed(TestData.buyer()),
				TestData.ticketType(TestData.publishedEvent(), 10));
	}

	@Test
	void checkInByEventSellerMarksTicketUsed() {
		when(ticketRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.checkIn(CODE, TestData.SELLER_EMAIL);

		assertThat(response.status()).isEqualTo(Ticket.Status.CHECKED_IN);
	}

	@Test
	void checkInByAnotherSellerIsForbidden() {
		when(ticketRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.checkIn(CODE, TestData.OTHER_SELLER_EMAIL))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.ISSUED);
		verify(ticketRepository, never()).save(any(Ticket.class));
	}

	@Test
	void checkInByBuyerIsForbidden() {
		when(ticketRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.checkIn(CODE, TestData.BUYER_EMAIL))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void checkInTwiceIsRejected() {
		ticket.markCheckedIn();
		when(ticketRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.checkIn(CODE, TestData.SELLER_EMAIL))
				.isInstanceOf(ApiException.class)
				.hasMessage("Ticket already used");
	}

	@Test
	void checkInOfCancelledTicketIsRejected() {
		ticket.markCancelled();
		when(ticketRepository.findByCodeForUpdate(CODE)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.checkIn(CODE, TestData.SELLER_EMAIL))
				.isInstanceOf(ApiException.class)
				.hasMessage("Ticket is not valid");
	}

	@Test
	void buyerAndSellerCanViewTicket() {
		when(ticketRepository.findByCode(CODE)).thenReturn(Optional.of(ticket));
		when(qrService.base64(ticket.getCode())).thenReturn("qr-image");

		assertThat(ticketService.findByCode(CODE, TestData.BUYER_EMAIL).qr()).isEqualTo("qr-image");
		assertThat(ticketService.findByCode(CODE, TestData.SELLER_EMAIL).qr()).isEqualTo("qr-image");
	}

	@Test
	void strangerSeesTicketAsNotFound() {
		when(ticketRepository.findByCode(CODE)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.findByCode(CODE, TestData.STRANGER_EMAIL))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
	}

}
