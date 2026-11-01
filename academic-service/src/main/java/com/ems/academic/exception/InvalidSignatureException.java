package com.ems.academic.exception;

public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException() { super("Invalid signature"); }
}