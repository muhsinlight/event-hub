package dev.takuma.event_hub.service;

public interface QrService {

	byte[] png(String text);

	String base64(String text);

}
