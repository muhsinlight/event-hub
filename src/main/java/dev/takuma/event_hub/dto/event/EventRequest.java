package dev.takuma.event_hub.dto.event;

import dev.takuma.event_hub.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventRequest(
		@NotBlank String name,
		@NotBlank String venue,
		@NotNull LocalDateTime startsAt,
		@NotNull Event.Status status
) {
}
