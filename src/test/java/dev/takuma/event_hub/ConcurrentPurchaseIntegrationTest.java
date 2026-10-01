package dev.takuma.event_hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import dev.takuma.event_hub.entity.Role;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

class ConcurrentPurchaseIntegrationTest extends ApiIntegrationTest {

	private static final int QUOTA = 5;
	private static final int BUYERS = 12;

	ConcurrentPurchaseIntegrationTest(MockMvc mockMvc) {
		super(mockMvc);
	}

	@Test
	void parallelPurchasesNeverExceedQuota() throws Exception {
		String seller = tokenFor(Role.SELLER);
		long eventId = createEvent(seller, "PUBLISHED");
		long ticketTypeId = createTicketType(seller, eventId, QUOTA);
		List<Callable<Integer>> purchases = IntStream.range(0, BUYERS)
				.mapToObj(index -> assertDoesNotThrow(() -> tokenFor(Role.USER)))
				.<Callable<Integer>>map(buyer -> () -> purchase(buyer, ticketTypeId, 1).andReturn().getResponse().getStatus())
				.toList();

		List<Integer> statuses;
		try (ExecutorService executor = Executors.newFixedThreadPool(BUYERS)) {
			statuses = executor.invokeAll(purchases).stream().map(result -> assertDoesNotThrow(() -> result.get())).toList();
		}

		assertThat(statuses).filteredOn(status -> status == 201).hasSize(QUOTA);
		assertThat(statuses).filteredOn(status -> status == 409).hasSize(BUYERS - QUOTA);
		assertThat(soldCount(eventId, ticketTypeId)).isEqualTo(QUOTA);
	}

}
