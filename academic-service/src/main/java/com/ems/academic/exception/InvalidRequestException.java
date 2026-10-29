package com.ems.academic.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}