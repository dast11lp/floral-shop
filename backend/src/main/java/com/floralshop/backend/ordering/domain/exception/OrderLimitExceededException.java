package com.floralshop.backend.ordering.domain.exception;

// Se lanza cuando una línea o el pedido completo superan los máximos que fijó la florista.
public class OrderLimitExceededException extends RuntimeException {

    public OrderLimitExceededException(String message) {
        super(message);
    }
}