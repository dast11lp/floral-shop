package com.floralshop.backend.catalog.domain.model;

// Lo que pesa una variante en el carrito: cuánto tarda y cuántos cupos ocupa.
// Trae los datos crudos (el tiempo de la variante, que puede ser null, y el de su producto)
// y la herencia la resuelve la misma regla que usa Product.
public record VariantWorkload(long variantId, Integer variantPreparationHours,
                              int productPreparationHours, int slotUnits) {

    public VariantWorkload {
        if (variantPreparationHours != null && variantPreparationHours < 0) {
            throw new IllegalArgumentException("El tiempo de preparación no puede ser negativo");
        }
        if (productPreparationHours < 0) {
            throw new IllegalArgumentException("El tiempo de preparación no puede ser negativo");
        }
        if (slotUnits <= 0) {
            throw new IllegalArgumentException("Una variante ocupa al menos un cupo");
        }
    }

    public int preparationHours() {
        return ProductVariant.effectivePreparationHours(variantPreparationHours, productPreparationHours);
    }
}