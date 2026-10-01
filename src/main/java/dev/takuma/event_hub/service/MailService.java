package dev.takuma.event_hub.service;

import java.util.List;

public interface MailService {

	void sendPurchaseConfirmation(PurchaseEmail email);

	record PurchaseEmail(
			String to,
			String buyerName,
			String eventName,
			String ticketTypeName,
			List<String> ticketCodes
	) {
	}

}
