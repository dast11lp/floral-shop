package com.floralshop.backend.payment.domain.model;

import java.util.Objects;

// Una combinación de pasarela y medio que se le puede ofrecer al cliente (ADR 0014, punto 1).
// holdMinutes: cuánto se aparta el cupo. priority: la de menor número es la sugerida.
public record PaymentMethodPolicy(String provider, PaymentMethod method, boolean enabled,
                                  int holdMinutes, int priority) {

    public PaymentMethodPolicy {
        Objects.requireNonNull(provider, "La pasarela es obligatoria");
        Objects.requireNonNull(method, "El medio de pago es obligatorio");
        if (provider.isBlank()) {
            throw new IllegalArgumentException("La pasarela no puede estar vacía");
        }
        if (holdMinutes <= 0 || priority <= 0) {
            throw new IllegalArgumentException("Los minutos de cupo y la prioridad deben ser mayores que cero");
        }
    }
}
