package com.floralshop.backend.payment.domain.model;

// Estados de un pago (ADR 0003). Solo PENDING tiene salidas; los demás son finales.
public enum PaymentStatus {
    PENDING, PAID, FAILED, EXPIRED, CANCELLED;

    public boolean canTransitionTo(PaymentStatus next) {
        return this == PENDING && next != PENDING;
    }
}