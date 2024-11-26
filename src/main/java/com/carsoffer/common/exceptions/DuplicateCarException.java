package com.carsoffer.common.exceptions;

public class DuplicateCarException extends RuntimeException {
    public DuplicateCarException(String message) {
        super(message);
    }
}