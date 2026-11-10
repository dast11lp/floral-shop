package com.floralshop.backend.scheduling.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

// Zona de entrega, con el costo de envío que se cobra en ella.
public record DeliveryZone(Long id, String name, BigDecimal shippingFee, boolean active) {

    public DeliveryZone {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(shippingFee, "El costo de envío es obligatorio");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (shippingFee.signum() < 0) {
            throw new IllegalArgumentException("El costo de envío no puede ser negativo");
        }
    }
}