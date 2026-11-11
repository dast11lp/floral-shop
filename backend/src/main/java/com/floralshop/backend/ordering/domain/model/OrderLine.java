package com.floralshop.backend.ordering.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

// Una línea del pedido, con la copia de la variante en el momento de la compra (ADR 0013, punto 2).
// variantId es solo un dato: no hay clave foránea hacia el catálogo.
public record OrderLine(long variantId, String productName, String variantName, BigDecimal unitPrice,
                        int quantity, int craftingHours, int slotUnits) {

    public OrderLine {
        Objects.requireNonNull(productName, "El nombre del producto es obligatorio");
        Objects.requireNonNull(variantName, "El nombre de la variante es obligatorio");
        Objects.requireNonNull(unitPrice, "El precio es obligatorio");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (craftingHours < 0 || slotUnits <= 0) {
            throw new IllegalArgumentException("Horas o cupos inválidos");
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}