package io.orion.app.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.*;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityFlowTest {

    private static final String PASSWORD = "S3cure-passphrase!";
    private static final String EMAIL = "admin@example.com";

    @Autowired MockMvc mvc;

    record Session(String slug, String access, String refresh) {}

    private String slug() { return "t-" + UUID.randomUUID().toString().substring(0, 8); }

    private Session register(String slug) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register-tenant").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantName":"%s","tenantSlug":"%s","adminEmail":"%s","adminName":"Admin","password":"%s"}
                    """.formatted(slug, slug, EMAIL, PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Session(slug, JsonPath.read(json, "$.accessToken"), JsonPath.read(json, "$.refreshToken"));
    }

    private ResultActions login(String slug, String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantSlug":"%s","email":"%s","password":"%s"}""".formatted(slug, email, password)));
    }

    private ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    private ResultActions createAnalyst(String accessToken) throws Exception {
        return mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","name":"Bob","password":"%s","roles":["ANALYST"]}""".formatted(PASSWORD)));
    }

    @Test
    void tenantsAreIsolated_andEmailsAreUniquePerTenant() throws Exception {
        Session a = register(slug());
        Session b = register(slug());

        createAnalyst(a.access()).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + a.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + b.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void loginIsScopedToTenant() throws Exception {
        Session a = register(slug());
        Session b = register(slug());
        login(a.slug(), EMAIL, PASSWORD).andExpect(status().isOk());
        login(a.slug(), EMAIL, "wrong-password-123").andExpect(status().isUnauthorized());
        login("does-not-exist", EMAIL, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotCreateUsers() throws Exception {
        Session a = register(slug());
        createAnalyst(a.access()).andExpect(status().isCreated());
        String bobToken = JsonPath.read(
                login(a.slug(), "bob@example.com", PASSWORD).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), "$.accessToken");
        createAnalyst(bobToken).andExpect(status().isForbidden());
    }

    @Test
    void refreshTokensRotate_andReuseRevokesEverything() throws Exception {
        Session s = register(slug());
        String r2 = JsonPath.read(refresh(s.refresh()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        refresh(s.refresh()).andExpect(status().isUnauthorized());
        refresh(r2).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }
}
