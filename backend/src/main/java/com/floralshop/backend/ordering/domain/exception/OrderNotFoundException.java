package com.floralshop.backend.ordering.domain.exception;

import java.util.UUID;

// No hay un pedido con ese código público.
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID publicCode) {
        super("No existe el pedido " + publicCode);
    }
}
