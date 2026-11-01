package com.ems.academic.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class Actors {

    private Actors() {}

    public static String name(Jwt jwt) {
        for (String claim : new String[]{"name", "preferred_username"}) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) {
                return value.length() > 100 ? value.substring(0, 100) : value;
            }
        }
        return jwt.getSubject();
    }
}