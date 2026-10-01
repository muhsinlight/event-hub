package dev.takuma.event_hub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.takuma.event_hub.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthSessionIntegrationTest extends ApiIntegrationTest {

	AuthSessionIntegrationTest(MockMvc mockMvc) {
		super(mockMvc);
	}

	@Test
	void loginReturnsAccessAndRefreshTokens() throws Exception {
		String email = register(Role.USER);

		perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.data.user.email").value(email));
	}

	@Test
	void refreshRotatesTokensAndRejectsOldRefresh() throws Exception {
		String email = register(Role.USER);
		MvcResult login = perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

		MvcResult refreshed = perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
				{ "refreshToken": "%s" }
				""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
				.andReturn();
		String newAccess = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.data.token");
		String newRefresh = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.data.refreshToken");

		perform(authorized(get("/api/orders"), newAccess)).andExpect(status().isOk());
		perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
				{ "refreshToken": "%s" }
				""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid refresh token"));
		perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
				{ "refreshToken": "%s" }
				""".formatted(newRefresh)))
				.andExpect(status().isOk());
	}

	@Test
	void logoutRevokesAccessTokenImmediately() throws Exception {
		String email = register(Role.USER);
		MvcResult login = perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		String access = JsonPath.read(login.getResponse().getContentAsString(), "$.data.token");
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.refreshToken");

		perform(authorized(post("/api/auth/logout"), access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logged out"));

		perform(authorized(get("/api/orders"), access))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Unauthorized"));
		perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
				{ "refreshToken": "%s" }
				""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutWithoutTokenIsUnauthorized() throws Exception {
		perform(post("/api/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Unauthorized"));
	}

	@Test
	void swaggerDocsRequireAdmin() throws Exception {
		perform(get("/v3/api-docs")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
		perform(authorized(get("/v3/api-docs"), tokenFor(Role.USER)))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/?denied"));
		perform(authorized(get("/v3/api-docs"), adminToken())).andExpect(status().isOk());
	}

}
