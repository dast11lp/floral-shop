package com.floralshop.backend.payment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.floralshop.backend.payment.domain.exception.InvalidPaymentTransitionException;

// Un pago (un intento de pago) de un pedido. Inmutable: un cambio devuelve un pago nuevo.
// providerReference: el id del cobro en la pasarela. Null mientras el cobro no existe allá (o en los manuales).
// checkoutUrl: adónde se manda al cliente a pagar. Null mientras el cobro no existe (o en los manuales).
// Hasta cuándo se retiene la franja no vive aquí: es del pedido (ADR 0012).
public record Payment(Long id, Long orderId, String provider, PaymentMethod method,
                      PaymentStatus status, BigDecimal amount, String providerReference, String checkoutUrl) {

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
        return new Payment(id, orderId, provider, method, next, amount, providerReference, checkoutUrl);
    }

    // El cobro ya se creó en la pasarela: se anotan su id allá y la URL de pago.
    public Payment withCheckout(String providerReference, String checkoutUrl) {
        Objects.requireNonNull(providerReference, "La referencia del cobro es obligatoria");
        Objects.requireNonNull(checkoutUrl, "La URL de pago es obligatoria");
        return new Payment(id, orderId, provider, method, status, amount, providerReference, checkoutUrl);
    }

    // Si es la misma opción que eligió el cliente (pasarela y medio).
    public boolean isFor(String provider, PaymentMethod method) {
        return this.provider.equals(provider) && this.method == method;
    }

    // Si el cobro ya existe en la pasarela. Un intento recién guardado todavía no lo tiene.
    public boolean hasCheckout() {
        return checkoutUrl != null;
    }
}
