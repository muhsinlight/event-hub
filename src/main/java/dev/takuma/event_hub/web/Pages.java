package dev.takuma.event_hub.web;

import dev.takuma.event_hub.utils.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class Pages {

	private Pages() {
	}

	static String redirectIfInvalid(BindingResult binding, RedirectAttributes redirect, String target) {
		if (!binding.hasErrors()) {
			return null;
		}
		redirect.addFlashAttribute("error", Forms.validationMessage(binding));
		return "redirect:" + target;
	}

	static void flashApiError(RedirectAttributes redirect, ApiException exception) {
		redirect.addFlashAttribute("error", exception.getMessage());
	}

	static void addApiError(Model model, ApiException exception) {
		model.addAttribute("error", exception.getMessage());
	}

	static void runOrFlash(RedirectAttributes redirect, Runnable action) {
		try {
			action.run();
		}
		catch (ApiException exception) {
			flashApiError(redirect, exception);
		}
	}

	static ModelAndView notFoundUnless(ApiException exception, String message, HttpStatus... statuses) {
		for (HttpStatus status : statuses) {
			if (exception.getStatus() == status) {
				return notFound(message);
			}
		}
		throw exception;
	}

	private static ModelAndView notFound(String message) {
		ModelAndView page = new ModelAndView("not-found", HttpStatus.NOT_FOUND);
		page.addObject("pageTitle", "Not found");
		page.addObject("message", message);
		return page;
	}

}
