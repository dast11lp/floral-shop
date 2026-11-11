package com.floralshop.backend.scheduling.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// Lo que la tienda le muestra a un carrito antes de pagar (ADR 0012, punto 1):
// cuánto tarda, desde cuándo se puede entregar y qué franjas alcanzan.
public record DeliveryOptions(int craftingHours, LocalDateTime earliestStart, List<SlotOption> slots) {

    public DeliveryOptions {
        Objects.requireNonNull(earliestStart, "El inicio más temprano es obligatorio");
        slots = List.copyOf(slots);
    }
}