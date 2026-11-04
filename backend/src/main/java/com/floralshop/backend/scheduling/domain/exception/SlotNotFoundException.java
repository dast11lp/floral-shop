package com.floralshop.backend.scheduling.domain.exception;

public class SlotNotFoundException extends RuntimeException {

    public SlotNotFoundException(long id) {
        super("No existe la franja con id " + id);
    }
}