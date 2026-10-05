package dev.takuma.event_hub.web;

public record PageAccount(String name, boolean seller, boolean admin) {

	public boolean buyer() {
		return true;
	}

}
