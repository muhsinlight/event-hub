package dev.takuma.event_hub.web;

import dev.takuma.event_hub.dto.order.OrderResponse;
import dev.takuma.event_hub.dto.order.PaymentRequest;
import dev.takuma.event_hub.dto.order.PurchaseRequest;
import dev.takuma.event_hub.service.OrderService;
import dev.takuma.event_hub.utils.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderPageController {

	private final OrderService orderService;

	public OrderPageController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/orders")
	public String list(
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
			Authentication authentication, Model model) {
		model.addAttribute("pageTitle", "Orders");
		model.addAttribute("orders", orderService.findByBuyerEmail(authentication.getName(), pageable));
		return "orders";
	}

	@PostMapping("/orders")
	public String purchase(@RequestParam Long eventId, @Valid PurchaseRequest request, BindingResult binding,
			Authentication authentication, RedirectAttributes redirect) {
		String eventPath = "/events/" + eventId;
		String invalid = Pages.redirectIfInvalid(binding, redirect, eventPath);
		if (invalid != null) {
			return invalid;
		}
		try {
			OrderResponse reserved = orderService.purchase(authentication.getName(), request);
			return "redirect:/orders/" + reserved.id() + "/pay";
		}
		catch (ApiException exception) {
			Pages.flashApiError(redirect, exception);
			return "redirect:" + eventPath;
		}
	}

	@GetMapping("/orders/{id}/pay")
	public ModelAndView payForm(@PathVariable Long id, Authentication authentication) {
		try {
			ModelAndView page = new ModelAndView("pay");
			page.addObject("pageTitle", "Pay");
			page.addObject("order", orderService.findOwned(id, authentication.getName()));
			return page;
		}
		catch (ApiException exception) {
			return Pages.notFoundUnless(exception, "Order not found", HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/orders/{id}/pay")
	public String pay(@PathVariable Long id, @Valid @ModelAttribute PaymentRequest paymentRequest, BindingResult binding,
			Authentication authentication, RedirectAttributes redirect) {
		String payPath = "/orders/" + id + "/pay";
		String invalid = Pages.redirectIfInvalid(binding, redirect, payPath);
		if (invalid != null) {
			return invalid;
		}
		try {
			redirect.addFlashAttribute("purchased", orderService.pay(id, authentication.getName(), paymentRequest));
			return "redirect:/orders";
		}
		catch (ApiException exception) {
			Pages.flashApiError(redirect, exception);
			return "redirect:" + payPath;
		}
	}

	@PostMapping("/orders/{id}/cancel")
	public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
		Pages.runOrFlash(redirect, () -> orderService.cancel(id, authentication.getName()));
		return "redirect:/orders";
	}

}
