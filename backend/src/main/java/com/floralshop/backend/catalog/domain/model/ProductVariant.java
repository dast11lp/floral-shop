package com.floralshop.backend.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

// Variante de un producto: un tamaño o presupuesto con su precio.
public record ProductVariant(Long id, String name, BigDecimal price, boolean active, int sortOrder) {

    public ProductVariant {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(price, "El precio es obligatorio");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }
}