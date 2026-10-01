package dev.takuma.event_hub;

import static org.assertj.core.api.Assertions.assertThat;

import dev.takuma.event_hub.service.MailService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@MockitoBean(types = MailService.class)
class ErrorResponseIntegrationTest {

	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	private final int port;

	ErrorResponseIntegrationTest(@LocalServerPort int port) {
		this.port = port;
	}

	@Test
	void malformedPathOnPublicRouteIsBadRequestNotUnauthorized() throws Exception {
		assertThat(get("/api/events/not-a-number").statusCode()).isEqualTo(400);
	}

	@Test
	void missingQueryParameterOnPublicRouteIsBadRequestNotUnauthorized() throws Exception {
		assertThat(get("/api/events/search").statusCode()).isEqualTo(400);
	}

	@Test
	void protectedRouteWithoutTokenIsUnauthorized() throws Exception {
		HttpResponse<String> response = get("/api/orders");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"message\":\"Unauthorized\"");
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
