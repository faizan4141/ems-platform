package com.ems.user.identity;

import com.ems.user.exception.IdentityProviderException;
import com.ems.user.exception.LoginAccountExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KeycloakIdentityClientTest {

    private static final String BASE = "http://kc.test";

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final NewLogin login = new NewLogin(tenantId, "a@b.com", "Asha", "Verma", "TEACHER");

    private MockRestServiceServer server;
    private KeycloakIdentityClient client;

    @BeforeEach
    void setUp() {
        KeycloakAdminProperties props = new KeycloakAdminProperties(
                BASE, "ems", "ems-user-service", "secret", "ems-web", "http://localhost:5173/", 3600);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakIdentityClient(builder.build(), props);
    }

    private void expectTokenRequest() {
        server.expect(requestTo(BASE + "/realms/ems/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void createUser_sendsTenantAndRequiredAction_assignsRole_andReusesCachedToken() {
        expectTokenRequest();   // expected exactly once: later calls must use the cached token
        server.expect(requestTo(BASE + "/admin/realms/ems/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok"))
                .andExpect(jsonPath("$.username").value("a@b.com"))
                .andExpect(jsonPath("$.attributes.tenant_id[0]").value(tenantId.toString()))
                .andExpect(jsonPath("$.requiredActions[0]").value("UPDATE_PASSWORD"))
                .andRespond(withCreatedEntity(URI.create(BASE + "/admin/realms/ems/users/" + userId)));
        server.expect(requestTo(BASE + "/admin/realms/ems/roles/TEACHER"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"role-1\",\"name\":\"TEACHER\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/admin/realms/ems/users/" + userId + "/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("TEACHER"))
                .andRespond(withNoContent());

        assertThat(client.createUser(login)).isEqualTo(userId);
        server.verify();
    }

    @Test
    void createUser_mapsKeycloakConflictToLoginAccountExists() {
        expectTokenRequest();
        server.expect(requestTo(BASE + "/admin/realms/ems/users"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.createUser(login))
                .isInstanceOf(LoginAccountExistsException.class);
    }

    @Test
    void createUser_deletesTheNewUserWhenRoleAssignmentFails() {
        expectTokenRequest();
        server.expect(requestTo(BASE + "/admin/realms/ems/users"))
                .andRespond(withCreatedEntity(URI.create(BASE + "/admin/realms/ems/users/" + userId)));
        server.expect(requestTo(BASE + "/admin/realms/ems/roles/TEACHER"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(BASE + "/admin/realms/ems/users/" + userId))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatThrownBy(() -> client.createUser(login))
                .isInstanceOf(IdentityProviderException.class);
        server.verify();
    }

    @Test
    void sendSetPasswordEmail_requestsUpdatePasswordAction() {
        expectTokenRequest();
        server.expect(requestTo(containsString("/admin/realms/ems/users/" + userId + "/execute-actions-email")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(queryParam("client_id", "ems-web"))
                .andExpect(queryParam("lifespan", "3600"))
                .andExpect(jsonPath("$[0]").value("UPDATE_PASSWORD"))
                .andRespond(withNoContent());

        client.sendSetPasswordEmail(userId);
        server.verify();
    }

    @Test
    void emailFailureSurfacesAsIdentityProviderException() {
        expectTokenRequest();
        server.expect(requestTo(containsString("/execute-actions-email")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sendSetPasswordEmail(userId))
                .isInstanceOf(IdentityProviderException.class);
    }
}