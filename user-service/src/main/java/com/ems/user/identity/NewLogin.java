package com.ems.user.identity;

import java.util.UUID;

public record NewLogin(UUID tenantId, String email, String firstName, String lastName, String realmRole) {
}