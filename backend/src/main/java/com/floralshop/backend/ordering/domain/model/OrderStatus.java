package com.floralshop.backend.ordering.domain.model;

import java.util.Objects;

// Estados de un pedido (ADR 0012) y sus salidas permitidas (ADR 0016, sección C).
// Pasar al mismo estado nunca es válido. Lo que no está aquí, no se permite.
// Cuándo se aplica cada cambio (el aviso, el job, la conciliación) lo decide la capa de aplicación (ADR 0009).
public enum OrderStatus {
    PENDING_PAYMENT,          // el cupo está apartado mientras se paga
    PENDING_PAYMENT_NO_SLOT,  // el pago sigue en revisión y la retención venció: el cupo se liberó (ADR 0016, A.2)
    CONFIRMED,                // el pago se aprobó: la florista lo arma y lo entrega
    CANCELLED,                // no hubo pago aprobado: el cupo se liberó
    PAID_WITHOUT_SLOT;        // el pago se aprobó cuando el cupo ya se había liberado: decide la florista

    public boolean canTransitionTo(OrderStatus next) {
        Objects.requireNonNull(next, "El estado destino es obligatorio");
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == PENDING_PAYMENT_NO_SLOT || next == CANCELLED;
            case PENDING_PAYMENT_NO_SLOT -> next == PAID_WITHOUT_SLOT || next == CANCELLED;
            // CONFIRMED: el ADR 0016 no define salidas (un reembolso no lo cambia, A.4).
            // CANCELLED: es final (A.6).
            // PAID_WITHOUT_SLOT: la gestión de la florista está pendiente (P10).
            case CONFIRMED, CANCELLED, PAID_WITHOUT_SLOT -> false;
        };
    }
}
