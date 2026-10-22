package com.ems.tenant.exception;

public class DuplicateTenantException extends RuntimeException {
    public DuplicateTenantException(String code) {
        super("Tenant code already exists: " + code);
    }
}