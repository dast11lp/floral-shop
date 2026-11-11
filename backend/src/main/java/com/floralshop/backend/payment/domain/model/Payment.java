package com.floralshop.backend.payment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.floralshop.backend.payment.domain.exception.InvalidPaymentTransitionException;

// Un pago de un pedido. Inmutable: un cambio de estado devuelve un pago nuevo.
// providerReference es la referencia del proveedor; puede ser null hasta que la entregue (o en los manuales).
public record Payment(Long id, Long orderId, String provider, PaymentMethod method,
                      PaymentStatus status, BigDecimal amount, String providerReference) {

    public Payment {
        Objects.requireNonNull(orderId, "El pedido es obligatorio");
        Objects.requireNonNull(provider, "El proveedor es obligatorio");
        Objects.requireNonNull(method, "El medio de pago es obligatorio");
        Objects.requireNonNull(status, "El estado es obligatorio");
        Objects.requireNonNull(amount, "El monto es obligatorio");
        if (provider.isBlank()) {
            throw new IllegalArgumentException("El proveedor no puede estar vacío");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
    }

    // Estricto: pasar al mismo estado, o salir de uno final, es un error.
    // "Ya estaba pagado" (confirmar dos veces) lo decide el caso de uso, no este modelo.
    public Payment transitionTo(PaymentStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidPaymentTransitionException(status, next);
        }
        return new Payment(id, orderId, provider, method, next, amount, providerReference);
    }
}