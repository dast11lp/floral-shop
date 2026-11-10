package com.floralshop.backend.scheduling.domain.exception;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}