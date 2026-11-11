package com.floralshop.backend.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

// Variante de un producto: un tamaño o presupuesto con su precio.
// craftingHours es opcional: si es null, la variante tarda lo mismo que su producto (ADR 0012).
// slotUnits es cuántos cupos de una franja ocupa cada unidad ("Cupos que ocupa" en la consola).
public record ProductVariant(Long id, String name, BigDecimal price, boolean active, int sortOrder,
                             Integer craftingHours, int slotUnits) {

    public ProductVariant {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(price, "El precio es obligatorio");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (craftingHours != null && craftingHours < 0) {
            throw new IllegalArgumentException("El tiempo de elaboración no puede ser negativo");
        }
        if (slotUnits <= 0) {
            throw new IllegalArgumentException("Una variante ocupa al menos un cupo");
        }
    }

    // La regla de herencia del tiempo, escrita UNA sola vez (ADR 0012, punto 2):
    // si la variante tiene su propio tiempo, ese (aunque sea 0); si no, el del producto.
    // La usan Product (para mostrar el tiempo en la tienda) y VariantWorkload (para el carrito).
    public static int effectiveCraftingHours(Integer variantHours, int productHours) {
        return variantHours != null ? variantHours : productHours;
    }
}