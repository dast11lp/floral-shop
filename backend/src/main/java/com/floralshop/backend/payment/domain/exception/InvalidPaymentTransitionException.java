package com.floralshop.backend.payment.domain.exception;

import com.floralshop.backend.payment.domain.model.PaymentStatus;

// Se lanza cuando se intenta un cambio de estado que el pago no permite.
public class InvalidPaymentTransitionException extends RuntimeException {

    public InvalidPaymentTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Un pago " + from + " no puede pasar a " + to);
    }
}