package dev.takuma.event_hub.service;

import dev.takuma.event_hub.dto.order.OrderResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.order.PaymentRequest;
import dev.takuma.event_hub.dto.order.PurchaseRequest;
import dev.takuma.event_hub.dto.ticket.TicketResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OrderService {

	OrderResponse purchase(String email, PurchaseRequest request);

	List<TicketResponse> pay(Long id, String email, PaymentRequest request);

	OrderResponse findOwned(Long id, String email);

	PageResponse<OrderResponse> findByBuyerEmail(String buyerEmail, Pageable pageable);

	OrderResponse cancel(Long id, String email);

}
