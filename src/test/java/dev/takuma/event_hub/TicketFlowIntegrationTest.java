package dev.takuma.event_hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class TicketFlowIntegrationTest extends ApiIntegrationTest {

	private final MailService mailService;

	private String seller;
	private String buyer;
	private long eventId;
	private long ticketTypeId;

	TicketFlowIntegrationTest(MockMvc mockMvc, MailService mailService) {
		super(mockMvc);
		this.mailService = mailService;
	}

	@BeforeEach
	void setUp() throws Exception {
		seller = tokenFor(Role.SELLER);
		buyer = tokenFor(Role.USER);
		eventId = createEvent(seller, "PUBLISHED");
		ticketTypeId = createTicketType(seller, eventId, 10);
	}

	@Test
	void purchaseReservesTicketsUntilCardIsApproved() throws Exception {
		MvcResult reserved = purchase(buyer, ticketTypeId, 3)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("Order reserved"))
				.andExpect(jsonPath("$.data.status").value("AWAITING_PAYMENT"))
				.andExpect(jsonPath("$.data.quantity").value(3))
				.andExpect(jsonPath("$.data.amount").value(75.0))
				.andReturn();
		assertThat(soldCount(eventId, ticketTypeId)).isEqualTo(3);
		verifyNoInteractions(mailService);

		long orderId = readId(reserved, "$.data.id");
		pay(buyer, orderId, "4000000000000002")
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.message").value("Payment declined"));
		verifyNoInteractions(mailService);

		pay(buyer, orderId, "4242424242424242")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].status").value("ISSUED"))
				.andExpect(jsonPath("$.data[0].qr").isNotEmpty());
		assertThat(soldCount(eventId, ticketTypeId)).isEqualTo(3);
		ArgumentCaptor<MailService.PurchaseEmail> sentEmail = ArgumentCaptor.forClass(MailService.PurchaseEmail.class);
		verify(mailService).sendPurchaseConfirmation(sentEmail.capture());
		assertThat(sentEmail.getValue().ticketCodes()).hasSize(3);
		assertThat(sentEmail.getValue().ticketTypeName()).isEqualTo("General");
	}

	@Test
	void purchaseAboveQuotaIsRejected() throws Exception {
		purchase(buyer, ticketTypeId, 11)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Not enough tickets"));

		assertThat(soldCount(eventId, ticketTypeId)).isZero();
	}

	@Test
	void draftEventIsNotOnSale() throws Exception {
		long draftId = createEvent(seller, "DRAFT");
		long draftTicketTypeId = createTicketType(seller, draftId, 10);

		purchase(buyer, draftTicketTypeId, 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Event is not on sale"));
	}

	@Test
	void ticketIsVisibleOnlyToBuyerAndEventSeller() throws Exception {
		String code = buyOne();

		perform(authorized(get("/api/tickets/{code}", code), buyer)).andExpect(status().isOk());
		perform(authorized(get("/api/tickets/{code}", code), seller)).andExpect(status().isOk());
		perform(authorized(get("/api/tickets/{code}", code), tokenFor(Role.USER)))
				.andExpect(status().isNotFound());
		perform(authorized(get("/api/tickets/{code}", code), tokenFor(Role.SELLER)))
				.andExpect(status().isNotFound());
	}

	@Test
	void qrImageIsPublic() throws Exception {
		String code = buyOne();

		perform(get("/api/tickets/{code}/qr", code))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG));
	}

	@Test
	void onlyEventSellerChecksInAndOnlyOnce() throws Exception {
		String code = buyOne();

		perform(authorized(post("/api/tickets/{code}/check-in", code), buyer))
				.andExpect(status().isForbidden());
		perform(authorized(post("/api/tickets/{code}/check-in", code), tokenFor(Role.SELLER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Ticket belongs to another seller's event"));

		perform(authorized(post("/api/tickets/{code}/check-in", code), seller))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CHECKED_IN"));
		perform(authorized(post("/api/tickets/{code}/check-in", code), seller))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ticket already used"));
	}

	@Test
	void cancelReleasesQuotaAndOnlyOwnerCanCancel() throws Exception {
		MvcResult reserved = purchase(buyer, ticketTypeId, 2).andExpect(status().isCreated()).andReturn();
		long orderId = readId(reserved, "$.data.id");
		MvcResult result = pay(buyer, orderId, "4242424242424242").andExpect(status().isCreated()).andReturn();
		String code = read(result, "$.data[0].code");

		perform(authorized(post("/api/orders/{id}/cancel", orderId), tokenFor(Role.USER)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Order belongs to another user"));

		perform(authorized(post("/api/orders/{id}/cancel", orderId), buyer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELLED"));
		assertThat(soldCount(eventId, ticketTypeId)).isZero();

		perform(authorized(post("/api/tickets/{code}/check-in", code), seller))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ticket is not valid"));
		perform(authorized(post("/api/orders/{id}/cancel", orderId), buyer))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Order already cancelled"));
	}

	@Test
	void buyerSeesOnlyOwnOrders() throws Exception {
		purchase(buyer, ticketTypeId, 1).andExpect(status().isCreated());

		perform(authorized(get("/api/orders"), buyer)).andExpect(jsonPath("$.data.content.length()").value(1));
		perform(authorized(get("/api/orders"), tokenFor(Role.USER)))
				.andExpect(jsonPath("$.data.content.length()").value(0));
	}

	private String buyOne() throws Exception {
		MvcResult reserved = purchase(buyer, ticketTypeId, 1).andExpect(status().isCreated()).andReturn();
		long orderId = readId(reserved, "$.data.id");
		MvcResult paid = pay(buyer, orderId, "4242424242424242").andExpect(status().isCreated()).andReturn();
		return read(paid, "$.data[0].code");
	}

}
