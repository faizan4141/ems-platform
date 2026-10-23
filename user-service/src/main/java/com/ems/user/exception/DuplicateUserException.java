package com.ems.user.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String email) {
        super("User already exists in this tenant: " + email);
    }
}