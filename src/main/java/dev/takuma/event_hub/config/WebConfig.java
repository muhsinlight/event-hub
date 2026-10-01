package dev.takuma.event_hub.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addFormatterForFieldType(LocalDateTime.class, new LocalDateTimeFormatter());
	}

	private static final class LocalDateTimeFormatter implements Formatter<LocalDateTime> {

		private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		@Override
		public LocalDateTime parse(String text, Locale locale) {
			String value = text.trim();
			try {
				return LocalDateTime.parse(value);
			}
			catch (DateTimeParseException exception) {
				return LocalDateTime.parse(value, MINUTES);
			}
		}

		@Override
		public String print(LocalDateTime value, Locale locale) {
			return value.toString();
		}

	}

}
