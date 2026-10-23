package com.ems.user.exception;

import java.util.UUID;

public class TenantNotActiveException extends RuntimeException {
    public TenantNotActiveException(UUID id) {
        super("Tenant is not active: " + id);
    }
}