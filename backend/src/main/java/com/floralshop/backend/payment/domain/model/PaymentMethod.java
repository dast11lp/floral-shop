package com.floralshop.backend.payment.domain.model;

// Cómo paga el cliente (ADR 0003). No es lo mismo que quién procesa el pago (el proveedor).
public enum PaymentMethod {
    CARD, PSE, NEQUI, CASH_POINT, MANUAL_TRANSFER, CASH_ON_DELIVERY;

    // Los manuales no pasan por una pasarela: los confirma una persona (ADR 0003, punto 3).
    public boolean isManual() {
        return this == MANUAL_TRANSFER || this == CASH_ON_DELIVERY;
    }
}