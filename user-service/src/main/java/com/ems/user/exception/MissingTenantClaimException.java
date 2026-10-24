package com.ems.user.exception;

public class MissingTenantClaimException extends RuntimeException {
    public MissingTenantClaimException() {
        super("Token has no valid tenant_id; this account is not linked to a tenant");
    }
}