package dev.takuma.event_hub.web;

public record PageAccount(boolean seller, boolean admin) {

	public boolean buyer() {
		return admin || !seller;
	}

}
