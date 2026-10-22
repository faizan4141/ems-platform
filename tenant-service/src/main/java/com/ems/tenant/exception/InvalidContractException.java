package com.ems.tenant.exception;

public class InvalidContractException extends RuntimeException {
    public InvalidContractException(String message) {
        super(message);
    }
}