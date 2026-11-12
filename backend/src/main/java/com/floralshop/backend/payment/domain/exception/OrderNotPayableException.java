package com.floralshop.backend.payment.domain.exception;

import java.util.UUID;

// Solo se paga un pedido pendiente de pago: uno confirmado o cancelado ya no se cobra.
public class OrderNotPayableException extends RuntimeException {

    public OrderNotPayableException(UUID orderCode) {
        super("El pedido " + orderCode + " no está pendiente de pago");
    }
}
