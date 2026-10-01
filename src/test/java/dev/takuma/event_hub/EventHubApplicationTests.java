package dev.takuma.event_hub;

import dev.takuma.event_hub.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = MailService.class)
class EventHubApplicationTests {

	@Test
	void contextLoads() {
	}

}
