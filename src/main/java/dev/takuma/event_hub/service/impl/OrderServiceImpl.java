package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.dto.order.OrderResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.order.PaymentRequest;
import dev.takuma.event_hub.dto.order.PurchaseRequest;
import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.entity.Order;
import dev.takuma.event_hub.entity.Payment; 
import dev.takuma.event_hub.entity.Ticket;
import dev.takuma.event_hub.entity.TicketType;
import dev.takuma.event_hub.entity.User;
import dev.takuma.event_hub.repository.OrderRepository;
import dev.takuma.event_hub.repository.PaymentRepository;
import dev.takuma.event_hub.repository.TicketRepository;
import dev.takuma.event_hub.repository.TicketTypeRepository;
import dev.takuma.event_hub.repository.UserRepository;
import dev.takuma.event_hub.service.MailService;
import dev.takuma.event_hub.service.OrderService;
import dev.takuma.event_hub.service.QrService;
import dev.takuma.event_hub.service.support.CardPayment;
import dev.takuma.event_hub.utils.ApiException;
import dev.takuma.event_hub.utils.TransactionHooks;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final TicketRepository ticketRepository;
	private final TicketTypeRepository ticketTypeRepository;
	private final PaymentRepository paymentRepository;
	private final QrService qrService;
	private final UserRepository userRepository;
	private final MailService mailService;
	private final Clock clock;

	public OrderServiceImpl(OrderRepository orderRepository, TicketRepository ticketRepository,
			TicketTypeRepository ticketTypeRepository, PaymentRepository paymentRepository, QrService qrService,
			UserRepository userRepository, MailService mailService, Clock clock) {
		this.orderRepository = orderRepository;
		this.ticketRepository = ticketRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.paymentRepository = paymentRepository;
		this.qrService = qrService;
		this.userRepository = userRepository;
		this.mailService = mailService;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> findByBuyerEmail(String buyerEmail, Pageable pageable) {
		return PageResponse.from(orderRepository.findByBuyerEmail(buyerEmail, pageable), OrderResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse findOwned(Long id, String email) {
		Order order = ApiException.orNotFound(orderRepository.findById(id), "Order not found");
		if (!order.ownedBy(email)) {
			throw ApiException.notFound("Order not found");
		}
		return OrderResponse.from(order);
	}

	@Override
	@Transactional
	public OrderResponse purchase(String email, PurchaseRequest request) {
		User buyer = ApiException.orNotFound(userRepository.findByEmail(email), "User not found");
		TicketType ticketType = ApiException.orNotFound(ticketTypeRepository.findByIdForUpdate(request.ticketTypeId()),
				"Ticket type not found");

		if (!ticketType.getEvent().isPublished()) {
			throw ApiException.conflict("Event is not on sale");
		}
		if (!ticketType.hasCapacity(request.quantity())) {
			throw ApiException.conflict("Not enough tickets");
		}

		ticketType.increaseSold(request.quantity());
		ticketTypeRepository.save(ticketType);
		Order order = orderRepository.save(Order.awaitingPayment(buyer, ticketType, request.quantity()));
		return OrderResponse.from(order);
	}

	@Override
	@Transactional
	public List<TicketResponse> pay(Long id, String email, PaymentRequest request) {
		Order order = lockedOrder(id, email);
		if (!order.isAwaitingPayment()) {
			throw ApiException.conflict("Order is not awaiting payment");
		}
		Instant now = clock.instant();
		CardPayment.Decision decision = CardPayment.authorize(request.cardNumber(), request.expiry(), request.cvc(),
				now);
		if (!decision.approved()) {
			throw ApiException.paymentDeclined("Payment declined");
		}

		paymentRepository.save(Payment.paid(order, decision.last4(), decision.brand(), now));
		TicketType ticketType = order.getTicketType();
		List<Ticket> tickets = new ArrayList<>();
		List<String> ticketCodes = new ArrayList<>();
		for (int i = 0; i < order.getQuantity(); i++) {
			Ticket saved = ticketRepository.save(Ticket.issued(order, ticketType));
			saved.attachQr(qrService.base64(saved.getCode()));
			tickets.add(saved);
			ticketCodes.add(saved.getCode());
		}
		order.markPaid();
		orderRepository.save(order);

		MailService.PurchaseEmail purchaseEmail = new MailService.PurchaseEmail(order.getBuyerEmail(),
				order.getBuyerName(), ticketType.getEvent().getName(), ticketType.getName(), ticketCodes);
		TransactionHooks.afterCommit(() -> mailService.sendPurchaseConfirmation(purchaseEmail));
		List<TicketResponse> responses = new ArrayList<>(tickets.size());
		for (Ticket ticket : tickets) {
			responses.add(TicketResponse.from(ticket));
		}
		return responses;
	}

	@Override
	@Transactional
	public OrderResponse cancel(Long id, String email) {
		Order order = lockedOrder(id, email);
		if (order.isCancelled()) {
			throw ApiException.conflict("Order already cancelled");
		}
		if (order.isAwaitingPayment()) {
			releaseReservation(order);
			order.markCancelled();
			return OrderResponse.from(orderRepository.save(order));
		}

		for (Ticket ticket : ticketRepository.findByOrderId(id)) {
			if (!ticket.isIssued()) {
				continue;
			}
			ticket.markCancelled();
			TicketType ticketType = ApiException.orNotFound(
					ticketTypeRepository.findByIdForUpdate(ticket.getTicketType().getId()), "Ticket type not found");
			ticketType.decreaseSold();
			ticketRepository.save(ticket);
			ticketTypeRepository.save(ticketType);
		}

		order.markCancelled();
		return OrderResponse.from(orderRepository.save(order));
	}

	private Order lockedOrder(Long id, String email) {
		Order order = ApiException.orNotFound(orderRepository.findByIdForUpdate(id), "Order not found");
		if (!order.ownedBy(email)) {
			throw ApiException.conflict("Order belongs to another user");
		}
		return order;
	}

	private void releaseReservation(Order order) {
		if (order.getTicketType() == null) {
			throw ApiException.conflict("Order has no ticket type");
		}
		TicketType ticketType = ApiException.orNotFound(
				ticketTypeRepository.findByIdForUpdate(order.getTicketType().getId()), "Ticket type not found");
		ticketType.releaseSold(order.getQuantity());
		ticketTypeRepository.save(ticketType);
	}

}
