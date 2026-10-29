package com.ems.academic.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}