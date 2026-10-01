package dev.takuma.event_hub.service.support;

import dev.takuma.event_hub.utils.ApiException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

public final class CardPayment {

	private CardPayment() {
	}

	public record Decision(boolean approved, String last4, String brand) {
	}

	public static Decision authorize(String cardNumber, String expiry, String cvc, Instant now) {
		String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");
		if (!digits.matches("\\d{13,19}") || !luhn(digits)) {
			throw ApiException.badRequest("Card number is invalid");
		}
		if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
			throw ApiException.badRequest("Expiry is invalid");
		}
		if (cvc == null || !cvc.matches("\\d{3,4}")) {
			throw ApiException.badRequest("CVC is invalid");
		}
		if (expired(expiry, now)) {
			throw ApiException.badRequest("Card is expired");
		}
		String last4 = digits.substring(digits.length() - 4);
		boolean approved = !digits.endsWith("0002");
		return new Decision(approved, last4, brand(digits));
	}

	private static boolean expired(String expiry, Instant now) {
		int month = Integer.parseInt(expiry.substring(0, 2));
		int year = 2000 + Integer.parseInt(expiry.substring(3));
		YearMonth card = YearMonth.of(year, month);
		YearMonth current = YearMonth.from(now.atZone(ZoneOffset.UTC));
		return card.isBefore(current);
	}

	private static String brand(String digits) {
		if (digits.startsWith("34") || digits.startsWith("37")) {
			return "Amex";
		}
		if (digits.startsWith("4")) {
			return "Visa";
		}
		if (digits.startsWith("5")) {
			return "Mastercard";
		}
		return "Card";
	}

	private static boolean luhn(String digits) {
		int sum = 0;
		boolean doubleDigit = false;
		for (int index = digits.length() - 1; index >= 0; index--) {
			int digit = digits.charAt(index) - '0';
			if (doubleDigit) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
			doubleDigit = !doubleDigit;
		}
		return sum % 10 == 0;
	}

}
