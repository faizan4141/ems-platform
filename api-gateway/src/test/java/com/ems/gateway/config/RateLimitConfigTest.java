package com.ems.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private final KeyResolver resolver = new RateLimitConfig().tenantKeyResolver();

    private ServerWebExchange exchangeFor(Jwt jwt) {
        MockServerWebExchange base = MockServerWebExchange.from(MockServerHttpRequest.get("/x").build());
        if (jwt == null) {
            return base;
        }
        return base.mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt, List.of())))
                .build();
    }

    @Test
    void usesTenantWhenPresent() {
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1")
                .claim("tenant_id", tenantId.toString()).build();

        assertThat(resolver.resolve(exchangeFor(jwt)).block()).isEqualTo("tenant:" + tenantId);
    }

    @Test
    void fallsBackToUserWhenNoTenant() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("platform-1").build();

        assertThat(resolver.resolve(exchangeFor(jwt)).block()).isEqualTo("user:platform-1");
    }

    @Test
    void usesAnonymousWhenNoPrincipal() {
        assertThat(resolver.resolve(exchangeFor(null)).block()).isEqualTo("anonymous");
    }
}