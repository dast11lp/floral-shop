package com.floralshop.backend.scheduling.domain.exception;

// Se lanza cuando se pide una zona que no existe o no está activa.
public class ZoneNotFoundException extends RuntimeException {

    public ZoneNotFoundException(long id) {
        super("No existe la zona de entrega con id " + id);
    }
}