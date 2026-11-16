package com.floralshop.backend.payment.domain.model;

import java.util.Objects;

// Estados de un pago (ADR 0003) y sus salidas permitidas (ADR 0016, sección B).
// Pasar al mismo estado nunca es válido. Lo que no está aquí, no se permite.
public enum PaymentStatus {
    PENDING, PAID, FAILED, EXPIRED, CANCELLED, REFUNDED;

    public boolean canTransitionTo(PaymentStatus next) {
        Objects.requireNonNull(next, "El estado destino es obligatorio");
        return switch (this) {
            // Se resuelve: aprobado, fallido, vencido o cancelado. Un reembolso de un pago pendiente
            // no cambia nada (ADR 0016, D.4, provisional hasta P9).
            case PENDING -> next == PAID || next == FAILED || next == EXPIRED || next == CANCELLED;
            // Un intento cerrado solo puede pasar a pagado, si la pasarela confirma después una aprobación
            // (ADR 0016, A.21). Que un FAILED necesite referencia lo revisa Payment, que la conoce.
            case FAILED, EXPIRED, CANCELLED -> next == PAID;
            // Un pago aprobado solo puede reembolsarse (ADR 0016, A.4).
            case PAID -> next == REFUNDED;
            case REFUNDED -> false;
        };
    }
}
