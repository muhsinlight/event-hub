package dev.takuma.event_hub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.takuma.event_hub.entity.Role;
import dev.takuma.event_hub.service.MailService;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoBean(types = MailService.class)
public abstract class ApiIntegrationTest {

	protected static final String PASSWORD = "secret123";
	protected static final String ADMIN_EMAIL = "admin@test.dev";

	private final MockMvc mockMvc;
	private String adminToken;

	protected ApiIntegrationTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	protected ResultActions perform(RequestBuilder request) throws Exception {
		return mockMvc.perform(request);
	}

	protected String register(Role role) throws Exception {
		if (role == Role.ADMIN) {
			return ADMIN_EMAIL;
		}
		String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.dev";
		MvcResult created = perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Test %s", "email": "%s", "password": "%s" }
				""".formatted(role, email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		if (role == Role.SELLER) {
			long id = readId(created, "$.data.id");
			perform(authorized(post("/api/users/{id}/role", id), adminToken()).contentType(MediaType.APPLICATION_JSON)
					.content("{\"role\":\"SELLER\"}"))
					.andExpect(status().isOk());
		}
		return email;
	}

	protected String adminToken() throws Exception {
		if (adminToken == null) {
			adminToken = login(ADMIN_EMAIL);
		}
		return adminToken;
	}

	protected String login(String email) throws Exception {
		MvcResult result = perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return read(result, "$.data.token");
	}

	protected String tokenFor(Role role) throws Exception {
		return login(register(role));
	}

	protected long createEvent(String token, String status) throws Exception {
		MvcResult result = createEventRequest(token, status).andExpect(status().isCreated()).andReturn();
		return readId(result, "$.data.id");
	}

	protected ResultActions createEventRequest(String token, String status) throws Exception {
		return perform(authorized(post("/api/events"), token).contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Event %s", "venue": "Arena", "startsAt": "2030-01-01T20:00:00", "status": "%s" }
				""".formatted(UUID.randomUUID(), status)));
	}

	protected long createTicketType(String token, long eventId, int quota) throws Exception {
		MvcResult result = createTicketTypeRequest(token, eventId, quota).andExpect(status().isCreated()).andReturn();
		return readId(result, "$.data.id");
	}

	protected ResultActions createTicketTypeRequest(String token, long eventId, int quota) throws Exception {
		return perform(authorized(post("/api/events/{eventId}/ticket-types", eventId), token)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{ "name": "General", "price": 25.00, "quota": %d }
						""".formatted(quota)));
	}

	protected ResultActions purchase(String token, long ticketTypeId, int quantity) throws Exception {
		return perform(authorized(post("/api/orders"), token).contentType(MediaType.APPLICATION_JSON).content("""
				{ "ticketTypeId": %d, "quantity": %d }
				""".formatted(ticketTypeId, quantity)));
	}

	protected ResultActions pay(String token, long orderId, String cardNumber) throws Exception {
		return perform(authorized(post("/api/orders/{id}/pay", orderId), token).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "cardNumber": "%s", "expiry": "12/30", "cvc": "123", "code": "EH-OK-001" }
						""".formatted(cardNumber)));
	}

	protected int soldCount(long eventId, long ticketTypeId) throws Exception {
		MvcResult result = perform(get("/api/events/{eventId}/ticket-types", eventId))
				.andExpect(status().isOk())
				.andReturn();
		List<Number> soldCounts = read(result, "$.data[?(@.id == " + ticketTypeId + ")].soldCount");
		return soldCounts.getFirst().intValue();
	}

	protected static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	protected static <T> T read(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), path);
	}

	protected static long readId(MvcResult result, String path) throws Exception {
		return ((Number) read(result, path)).longValue();
	}

}
