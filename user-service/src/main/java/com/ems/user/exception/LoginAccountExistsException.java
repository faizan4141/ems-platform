package com.ems.user.exception;

public class LoginAccountExistsException extends RuntimeException {
    public LoginAccountExistsException(String email) {
        super("A login already exists on the platform for " + email);
    }
}