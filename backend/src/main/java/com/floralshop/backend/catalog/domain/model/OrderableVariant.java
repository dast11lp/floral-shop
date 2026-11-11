package com.floralshop.backend.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

// Una variante que se puede pedir, con lo que el pedido copia en el momento de la compra (ADR 0013, punto 2):
// nombres, precio, horas de elaboración y cupos que ocupa.
public record OrderableVariant(long variantId, String productName, String variantName, BigDecimal price,
                               Integer variantCraftingHours, int productCraftingHours, int slotUnits) {

    public OrderableVariant {
        Objects.requireNonNull(productName, "El nombre del producto es obligatorio");
        Objects.requireNonNull(variantName, "El nombre de la variante es obligatorio");
        Objects.requireNonNull(price, "El precio es obligatorio");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (slotUnits <= 0) {
            throw new IllegalArgumentException("Una variante ocupa al menos un cupo");
        }
    }

    public int craftingHours() {
        return ProductVariant.effectiveCraftingHours(variantCraftingHours, productCraftingHours);
    }
}