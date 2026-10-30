package com.ems.academic.exception;

public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) { super(message); }
}