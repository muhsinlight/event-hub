package dev.takuma.event_hub;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

class SecurityIntegrationTest extends ApiIntegrationTest {

	private final UserRepository userRepository;

	SecurityIntegrationTest(MockMvc mockMvc, UserRepository userRepository) {
		super(mockMvc);
		this.userRepository = userRepository;
	}

	@Test
	void pagePostWithoutCsrfTokenIsRejected() throws Exception {
		perform(post("/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", "buyer@test.dev")
				.param("password", PASSWORD))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/?denied"));
	}

	@Test
	void homePageIsPublic() throws Exception {
		perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("On the board")));
	}

	@Test
	void homePageListsPublishedEventsAndHidesDrafts() throws Exception {
		String seller = tokenFor(Role.SELLER);
		long publishedId = createEvent(seller, "PUBLISHED");
		long draftId = createEvent(seller, "DRAFT");

		perform(get("/").param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/events/" + publishedId)))
				.andExpect(content().string(not(containsString("/events/" + draftId))));

		perform(get("/events/{id}", publishedId))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tickets")));
		perform(get("/events/{id}", draftId)).andExpect(status().isNotFound());
	}

	@Test
	void loginPageOpensOrdersForTheBuyer() throws Exception {
		String email = register(Role.USER);
		perform(get("/orders")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));

		MvcResult login = perform(post("/login").with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", email)
				.param("password", PASSWORD))
				.andExpect(status().isFound())
				.andReturn();
		String setCookie = login.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
				.filter(value -> value.startsWith("access_token="))
				.findFirst()
				.orElseThrow();
		String token = setCookie.substring("access_token=".length(), setCookie.indexOf(';'));

		perform(get("/orders").header(HttpHeaders.COOKIE, "access_token=" + token))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Orders")));
		perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("Register")));
		perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(containsString("Create account")));
	}

	@Test
	void registrationIsAlwaysUserAndOnlyAdminGrantsSeller() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@test.dev";
		MvcResult created = perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Buyer", "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.role").value("USER"))
				.andReturn();
		long id = readId(created, "$.data.id");
		String buyer = login(email);

		perform(authorized(post("/api/users/{id}/role", id), buyer).contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"SELLER\"}"))
				.andExpect(status().isForbidden());
		perform(authorized(post("/api/users/{id}/role", id), adminToken()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"SELLER\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("SELLER"));
	}

	@Test
	void anonymousCannotCreateEvent() throws Exception {
		perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Unauthorized"));
	}

	@Test
	void buyerCannotCreateEvent() throws Exception {
		createEventRequest(tokenFor(Role.USER), "PUBLISHED")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Forbidden"));
	}

	@Test
	void sellerCannotAddTicketTypeToAnotherSellersEvent() throws Exception {
		long eventId = createEvent(tokenFor(Role.SELLER), "PUBLISHED");

		createTicketTypeRequest(tokenFor(Role.SELLER), eventId, 10)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Event belongs to another seller"));
	}

	@Test
	void sellerCannotBuyTickets() throws Exception {
		String seller = tokenFor(Role.SELLER);
		long eventId = createEvent(seller, "PUBLISHED");
		long ticketTypeId = createTicketType(seller, eventId, 10);

		purchase(seller, ticketTypeId, 1).andExpect(status().isForbidden());
	}

	@Test
	void draftIsHiddenFromEveryoneButItsSeller() throws Exception {
		String owner = tokenFor(Role.SELLER);
		long draftId = createEvent(owner, "DRAFT");
		int id = (int) draftId;

		perform(get("/api/events/{id}", draftId)).andExpect(status().isNotFound());
		perform(authorized(get("/api/events/{id}", draftId), tokenFor(Role.SELLER)))
				.andExpect(status().isNotFound());
		perform(get("/api/events/{id}/ticket-types", draftId)).andExpect(status().isNotFound());
		perform(get("/api/events")).andExpect(jsonPath("$.data.content[*].id", not(hasItem(id))));

		perform(authorized(get("/api/events/{id}", draftId), owner)).andExpect(status().isOk());
		perform(authorized(get("/api/events").param("size", "100"), owner))
				.andExpect(jsonPath("$.data.content[*].id", hasItem(id)));
	}

	@Test
	void publishedEventIsPublic() throws Exception {
		long eventId = createEvent(tokenFor(Role.SELLER), "PUBLISHED");

		perform(get("/api/events/{id}", eventId)).andExpect(status().isOk());
		perform(get("/api/events/{id}/ticket-types", eventId)).andExpect(status().isOk());
	}

	@Test
	void unknownEmailAndWrongPasswordGetTheSameAnswer() throws Exception {
		String email = register(Role.USER);

		perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "wrong-password" }
				""".formatted(email)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
		perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "nobody@test.dev", "password": "wrong-password" }
				"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void tokenOfDeletedUserIsUnauthorized() throws Exception {
		String email = register(Role.USER);
		String token = login(email);
		userRepository.delete(userRepository.findByEmail(email).orElseThrow());

		perform(authorized(get("/api/orders"), token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Unauthorized"));
	}

	@Test
	void healthIsPublicAndActuatorDetailsRequireAdmin() throws Exception {
		perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());

		perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Unauthorized"));
		perform(authorized(get("/actuator/info"), tokenFor(Role.USER)))
				.andExpect(status().isForbidden());
		perform(authorized(get("/actuator/health"), tokenFor(Role.USER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components").doesNotExist());

		perform(authorized(get("/actuator/health"), adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.db.status").value("UP"));
		perform(authorized(get("/actuator/info"), adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.build.artifact").value("event-hub"));
		perform(authorized(get("/actuator/env"), adminToken())).andExpect(status().isNotFound());
	}

	@Test
	void tamperedTokenIsUnauthorized() throws Exception {
		String token = tokenFor(Role.USER);
		String forged = token.substring(0, token.lastIndexOf('.') + 1) + "forged-signature";

		perform(authorized(get("/api/orders"), forged)).andExpect(status().isUnauthorized());
	}

}
