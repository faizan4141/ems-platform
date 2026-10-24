package com.ems.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class RateLimitConfig {

    /**
     * Decides whose "bucket" a request counts against: the caller's tenant if the
     * token has one, else the individual user (e.g. platform admins), else "anonymous".
     */
    @Bean
    KeyResolver tenantKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(auth -> {
                    String tenant = auth.getToken().getClaimAsString("tenant_id");
                    return tenant != null ? "tenant:" + tenant : "user:" + auth.getName();
                })
                .defaultIfEmpty("anonymous");
    }
}