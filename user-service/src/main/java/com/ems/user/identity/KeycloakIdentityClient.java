package com.ems.user.identity;

import com.ems.user.exception.IdentityProviderException;
import com.ems.user.exception.LoginAccountExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class KeycloakIdentityClient implements IdentityProvisioner {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final KeycloakAdminProperties props;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakIdentityClient(RestClient keycloakRestClient, KeycloakAdminProperties props) {
        this.restClient = keycloakRestClient;
        this.props = props;
    }

    @Override
    public UUID createUser(NewLogin login) {
        UUID id = createKeycloakUser(login);
        try {
            assignRealmRole(id, login.realmRole());
        } catch (RuntimeException e) {
            deleteQuietly(id);   // never leave a half-configured login behind
            throw e;
        }
        return id;
    }

    @Override
    public void sendSetPasswordEmail(UUID identityUserId) {
        URI uri = UriComponentsBuilder.fromUriString(props.baseUrl())
                .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                .queryParam("client_id", props.inviteClientId())
                .queryParam("redirect_uri", props.inviteRedirectUri())
                .queryParam("lifespan", props.inviteLifespanSeconds())
                .buildAndExpand(props.realm(), identityUserId)
                .encode()
                .toUri();
        try {
            restClient.put().uri(uri).headers(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new IdentityProviderException("Keycloak could not send the invitation email", e);
        }
    }

    @Override
    public void deleteUser(UUID identityUserId) {
        try {
            restClient.delete().uri(adminUri("/users/{id}", identityUserId)).headers(auth())
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // already gone: that's the outcome we wanted
        } catch (RestClientException e) {
            throw new IdentityProviderException("Keycloak could not delete the user", e);
        }
    }

    // ---- internals ----

    private UUID createKeycloakUser(NewLogin login) {
        Map<String, Object> payload = Map.of(
                "username", login.email(),
                "email", login.email(),
                "firstName", login.firstName(),
                "lastName", login.lastName(),
                "enabled", true,
                "emailVerified", true,   // no password exists until they use the emailed link
                "attributes", Map.of("tenant_id", List.of(login.tenantId().toString())),
                "requiredActions", List.of("UPDATE_PASSWORD"));
        try {
            ResponseEntity<Void> response = restClient.post().uri(adminUri("/users")).headers(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve().toBodilessEntity();
            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new IdentityProviderException("Keycloak did not return the new user's location", null);
            }
            String path = location.getPath();
            return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        } catch (HttpClientErrorException.Conflict e) {
            throw new LoginAccountExistsException(login.email());
        } catch (RestClientException e) {
            throw new IdentityProviderException("Keycloak could not create the user", e);
        }
    }

    private void assignRealmRole(UUID userId, String roleName) {
        try {
            Map<String, Object> role = restClient.get().uri(adminUri("/roles/{name}", roleName))
                    .headers(auth()).retrieve().body(MAP_TYPE);
            restClient.post().uri(adminUri("/users/{id}/role-mappings/realm", userId)).headers(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(Map.of("id", role.get("id"), "name", role.get("name"))))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new IdentityProviderException("Keycloak could not assign role " + roleName, e);
        }
    }

    private void deleteQuietly(UUID id) {
        try {
            deleteUser(id);
        } catch (RuntimeException e) {
            log.error("Could not clean up Keycloak user {}; delete it manually", id, e);
        }
    }

    private Consumer<HttpHeaders> auth() {
        String token = accessToken();
        return headers -> headers.setBearerAuth(token);
    }

    private URI adminUri(String path, Object... vars) {
        Object[] all = new Object[vars.length + 1];
        all[0] = props.realm();
        System.arraycopy(vars, 0, all, 1, vars.length);
        return UriComponentsBuilder.fromUriString(props.baseUrl())
                .path("/admin/realms/{realm}" + path)
                .buildAndExpand(all).encode().toUri();
    }

    /**
     * Client-credentials token for our service account, cached until shortly before expiry.
     */
    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        if (props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IdentityProviderException(
                    "KEYCLOAK_ADMIN_CLIENT_SECRET is not configured for user-service", null);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        try {
            Map<String, Object> body = restClient.post()
                    .uri(URI.create(props.baseUrl() + "/realms/" + props.realm() + "/protocol/openid-connect/token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(MAP_TYPE);
            long ttl = ((Number) body.get("expires_in")).longValue();
            cachedToken = (String) body.get("access_token");
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(ttl - 30, 1));
            return cachedToken;
        } catch (RestClientException e) {
            throw new IdentityProviderException("Could not obtain a Keycloak admin token", e);
        }
    }
}