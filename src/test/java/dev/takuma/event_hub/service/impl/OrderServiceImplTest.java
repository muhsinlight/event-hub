package dev.takuma.event_hub.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.TestData;
import dev.takuma.event_hub.dto.order.OrderResponse;
import dev.takuma.event_hub.dto.order.PaymentRequest;
import dev.takuma.event_hub.dto.order.PurchaseRequest;
import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.entity.Event;
import dev.takuma.event_hub.entity.Order;
import dev.takuma.event_hub.entity.Payment;
import dev.takuma.event_hub.entity.Ticket;
import dev.takuma.event_hub.entity.TicketType;
import dev.takuma.event_hub.repository.CheckoutCodeRepository;
import dev.takuma.event_hub.repository.OrderRepository;
import dev.takuma.event_hub.repository.PaymentRepository;
import dev.takuma.event_hub.repository.TicketRepository;
import dev.takuma.event_hub.repository.TicketTypeRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.service.MailService;
import dev.takuma.event_hub.service.QrService;
import dev.takuma.event_hub.utils.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	private static final Long TICKET_TYPE_ID = 7L;
	private static final Long ORDER_ID = 11L;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private TicketTypeRepository ticketTypeRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private CheckoutCodeRepository checkoutCodeRepository;

	@Mock
	private QrService qrService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private MailService mailService;

	@Mock
	private PurchaseRequest request;

	@Mock
	private PaymentRequest paymentRequest;

	@Mock
	private Clock clock;

	@InjectMocks
	private OrderServiceImpl orderService;

	@Test
	void purchaseReservesQuotaUntilPayment() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		givenBuyerAndTicketType(ticketType);
		when(request.quantity()).thenReturn(3);
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());

		OrderResponse order = orderService.purchase(TestData.BUYER_EMAIL, request);

		assertThat(order.status()).isEqualTo(Order.Status.AWAITING_PAYMENT);
		assertThat(order.quantity()).isEqualTo(3);
		assertThat(order.amount()).isEqualByComparingTo("30");
		assertThat(ticketType.getSoldCount()).isEqualTo(3);
		verify(ticketTypeRepository).save(ticketType);
		verify(ticketRepository, never()).save(any(Ticket.class));
		verify(mailService, never()).sendPurchaseConfirmation(any(MailService.PurchaseEmail.class));
	}

	@Test
	void payIssuesTicketsAfterApprovedCard() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		Order order = Order.awaitingPayment(TestData.buyer(), ticketType, 2);
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
		when(paymentRequest.cardNumber()).thenReturn("4242424242424242");
		when(paymentRequest.expiry()).thenReturn("12/30");
		when(paymentRequest.cvc()).thenReturn("123");
		when(paymentRequest.code()).thenReturn("EH-OK-001");
		when(checkoutCodeRepository.existsByCodeIgnoreCase("EH-OK-001")).thenReturn(true);
		when(clock.instant()).thenReturn(Instant.parse("2026-09-30T12:00:00Z"));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(returnsFirstArg());
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(returnsFirstArg());
		when(qrService.base64(anyString())).thenReturn("qr-image");
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());

		List<TicketResponse> tickets = orderService.pay(ORDER_ID, TestData.BUYER_EMAIL, paymentRequest);

		assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);
		assertThat(tickets).hasSize(2)
				.allSatisfy(ticket -> {
					assertThat(ticket.status()).isEqualTo(Ticket.Status.ISSUED);
					assertThat(ticket.qr()).isEqualTo("qr-image");
				});
		assertThat(tickets).extracting(ticket -> ticket.code()).doesNotHaveDuplicates();
		verify(mailService).sendPurchaseConfirmation(any(MailService.PurchaseEmail.class));
	}

	@Test
	void payDeclinesTestCardWithoutIssuingTickets() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		Order order = Order.awaitingPayment(TestData.buyer(), ticketType, 1);
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
		when(paymentRequest.cardNumber()).thenReturn("4000000000000002");
		when(paymentRequest.expiry()).thenReturn("12/30");
		when(paymentRequest.cvc()).thenReturn("123");
		when(paymentRequest.code()).thenReturn("EH-OK-001");
		when(checkoutCodeRepository.existsByCodeIgnoreCase("EH-OK-001")).thenReturn(true);
		when(clock.instant()).thenReturn(Instant.parse("2026-09-30T12:00:00Z"));

		assertThatThrownBy(() -> orderService.pay(ORDER_ID, TestData.BUYER_EMAIL, paymentRequest))
				.isInstanceOf(ApiException.class)
				.hasMessage("Payment declined")
				.extracting("status").isEqualTo(HttpStatus.PAYMENT_REQUIRED);
		assertThat(order.getStatus()).isEqualTo(Order.Status.AWAITING_PAYMENT);
		verify(ticketRepository, never()).save(any(Ticket.class));
		verify(mailService, never()).sendPurchaseConfirmation(any(MailService.PurchaseEmail.class));
	}

	@Test
	void payRejectsUnknownCheckoutCode() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		Order order = Order.awaitingPayment(TestData.buyer(), ticketType, 1);
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
		when(paymentRequest.code()).thenReturn("NOPE");
		when(checkoutCodeRepository.existsByCodeIgnoreCase("NOPE")).thenReturn(false);

		assertThatThrownBy(() -> orderService.pay(ORDER_ID, TestData.BUYER_EMAIL, paymentRequest))
				.isInstanceOf(ApiException.class)
				.hasMessage("Invalid checkout code")
				.extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
		verify(ticketRepository, never()).save(any(Ticket.class));
	}

	@Test
	void purchaseRejectsEventThatIsNotPublished() {
		givenBuyerAndTicketType(TestData.ticketType(TestData.draftEvent(), 10));

		assertThatThrownBy(() -> orderService.purchase(TestData.BUYER_EMAIL, request))
				.isInstanceOf(ApiException.class)
				.hasMessage("Event is not on sale")
				.extracting("status").isEqualTo(HttpStatus.CONFLICT);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void purchaseRejectsQuantityAboveRemainingQuota() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 5);
		ticketType.increaseSold(4);
		givenBuyerAndTicketType(ticketType);
		when(request.quantity()).thenReturn(2);

		assertThatThrownBy(() -> orderService.purchase(TestData.BUYER_EMAIL, request))
				.isInstanceOf(ApiException.class)
				.hasMessage("Not enough tickets");
		assertThat(ticketType.getSoldCount()).isEqualTo(4);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	void purchaseFailsForUnknownUser() {
		when(userRepository.findByEmail(TestData.BUYER_EMAIL)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.purchase(TestData.BUYER_EMAIL, request))
				.isInstanceOf(ApiException.class)
				.extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void cancelRejectsOrderOfAnotherUser() {
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(Order.confirmed(TestData.buyer())));

		assertThatThrownBy(() -> orderService.cancel(ORDER_ID, TestData.STRANGER_EMAIL))
				.isInstanceOf(ApiException.class)
				.hasMessage("Order belongs to another user");
	}

	@Test
	void cancelRejectsAlreadyCancelledOrder() {
		Order order = Order.confirmed(TestData.buyer());
		order.markCancelled();
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.cancel(ORDER_ID, TestData.BUYER_EMAIL))
				.isInstanceOf(ApiException.class)
				.hasMessage("Order already cancelled");
	}

	@Test
	void cancelReleasesReservationBeforePayment() {
		TicketType ticketType = TestData.ticketType(TestData.publishedEvent(), 10);
		ticketType.increaseSold(2);
		Order order = Order.awaitingPayment(TestData.buyer(), ticketType, 2);
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
		when(ticketTypeRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(ticketType));
		when(orderRepository.save(order)).thenReturn(order);

		OrderResponse response = orderService.cancel(ORDER_ID, TestData.BUYER_EMAIL);

		assertThat(response.status()).isEqualTo(Order.Status.CANCELLED);
		assertThat(ticketType.getSoldCount()).isZero();
		verify(ticketRepository, never()).save(any(Ticket.class));
	}

	@Test
	void cancelReleasesOnlyIssuedTickets() {
		Event event = TestData.publishedEvent();
		TicketType ticketType = TestData.ticketType(event, 10);
		ticketType.increaseSold(2);
		Order order = Order.confirmed(TestData.buyer());
		Ticket issued = TestData.issuedTicket(order, ticketType);
		Ticket checkedIn = TestData.issuedTicket(order, ticketType);
		checkedIn.markCheckedIn();
		when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
		when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(issued, checkedIn));
		when(ticketTypeRepository.findByIdForUpdate(nullable(Long.class))).thenReturn(Optional.of(ticketType));
		when(orderRepository.save(order)).thenReturn(order);

		OrderResponse response = orderService.cancel(ORDER_ID, TestData.BUYER_EMAIL);

		assertThat(response.status()).isEqualTo(Order.Status.CANCELLED);
		assertThat(issued.getStatus()).isEqualTo(Ticket.Status.CANCELLED);
		assertThat(checkedIn.getStatus()).isEqualTo(Ticket.Status.CHECKED_IN);
		assertThat(ticketType.getSoldCount()).isEqualTo(1);
	}

	private void givenBuyerAndTicketType(TicketType ticketType) {
		when(userRepository.findByEmail(TestData.BUYER_EMAIL)).thenReturn(Optional.of(TestData.buyer()));
		when(request.ticketTypeId()).thenReturn(TICKET_TYPE_ID);
		when(ticketTypeRepository.findByIdForUpdate(TICKET_TYPE_ID)).thenReturn(Optional.of(ticketType));
	}

}
