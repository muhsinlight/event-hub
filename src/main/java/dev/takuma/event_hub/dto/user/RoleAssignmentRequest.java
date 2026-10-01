package dev.takuma.event_hub.dto.user;

import dev.takuma.event_hub.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleAssignmentRequest(@NotNull Role role) {
}
