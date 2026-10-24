package com.ems.user.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String baseUrl,
        String realm,
        String clientId,
        String clientSecret,
        String inviteClientId,
        String inviteRedirectUri,
        int inviteLifespanSeconds) {
}