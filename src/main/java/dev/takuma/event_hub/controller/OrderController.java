package dev.takuma.event_hub.controller;

import dev.takuma.event_hub.dto.order.OrderResponse;
import dev.takuma.event_hub.dto.common.PageResponse;
import dev.takuma.event_hub.dto.order.PaymentRequest;
import dev.takuma.event_hub.dto.order.PurchaseRequest;
import dev.takuma.event_hub.dto.ticket.TicketResponse;
import dev.takuma.event_hub.service.OrderService;
import dev.takuma.event_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> purchase(@Valid @RequestBody PurchaseRequest request,
			Authentication authentication) {
		return ApiResponse.<OrderResponse>builder()
				.status(HttpStatus.CREATED)
				.message("Order reserved")
				.data(orderService.purchase(authentication.getName(), request))
				.response();
	}

	@PostMapping("/{id}/pay")
	public ResponseEntity<ApiResponse<List<TicketResponse>>> pay(@PathVariable Long id,
			@Valid @RequestBody PaymentRequest request, Authentication authentication) {
		return ApiResponse.<List<TicketResponse>>builder()
				.status(HttpStatus.CREATED)
				.message("Tickets purchased")
				.data(orderService.pay(id, authentication.getName(), request))
				.response();
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> findMine(
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication) {
		return ApiResponse.<PageResponse<OrderResponse>>builder()
				.data(orderService.findByBuyerEmail(authentication.getName(), pageable))
				.response();
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id, Authentication authentication) {
		return ApiResponse.<OrderResponse>builder()
				.message("Order cancelled")
				.data(orderService.cancel(id, authentication.getName()))
				.response();
	}

}
