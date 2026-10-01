package dev.takuma.event_hub.service.impl;

import dev.takuma.event_hub.service.MailService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Profile("!test")
public class ResendMailService implements MailService {

	private static final Logger log = LoggerFactory.getLogger(ResendMailService.class);

	private final RestClient restClient;
	private final String from;

	public ResendMailService(RestClient.Builder restClientBuilder,
			@Value("${app.resend.api-key}") String apiKey,
			@Value("${app.resend.from}") String from) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("RESEND_API_KEY is required");
		}
		if (from == null || from.isBlank()) {
			throw new IllegalStateException("RESEND_FROM is required");
		}
		this.from = from;
		this.restClient = restClientBuilder
				.baseUrl("https://api.resend.com")
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.build();
	}

	@Override
	public void sendPurchaseConfirmation(PurchaseEmail email) {
		String codes = String.join(", ", email.ticketCodes());
		String text = """
				Hi %s,

				Your tickets for %s (%s) are confirmed.

				Ticket codes: %s

				Show a ticket code or its QR at the door.
				""".formatted(email.buyerName(), email.eventName(), email.ticketTypeName(), codes);
		try {
			restClient.post()
					.uri("/emails")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"from", from,
							"to", List.of(email.to()),
							"subject", "Your tickets for " + email.eventName(),
							"text", text))
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException exception) {
			log.error("Purchase confirmation email failed for {}", email.to(), exception);
		}
	}

}
