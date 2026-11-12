package com.floralshop.backend.payment.domain.exception;

import com.floralshop.backend.payment.domain.model.PaymentMethod;

// La combinación de pasarela y medio no está habilitada, o su pasarela no tiene credenciales (ADR 0014).
public class PaymentOptionNotAvailableException extends RuntimeException {

    public PaymentOptionNotAvailableException(String provider, PaymentMethod method) {
        super("La opción de pago " + provider + " con " + method + " no está disponible");
    }
}
