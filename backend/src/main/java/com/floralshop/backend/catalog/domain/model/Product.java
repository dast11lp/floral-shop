package com.floralshop.backend.catalog.domain.model;

import java.util.List;
import java.util.Objects;

// Producto del catálogo, con sus variantes y sus categorías.
// Es inmutable: las listas se copian al crear el objeto y nadie puede modificarlas después.
// preparationHours es lo que tarda en armarse, en horas (ADR 0012). Es obligatorio.
public record Product(Long id, String name, String slug, String description, boolean active,
                      int preparationHours, List<ProductVariant> variants, List<Category> categories) {

    public Product {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(slug, "El slug es obligatorio");
        Objects.requireNonNull(description, "La descripción es obligatoria");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (slug.isBlank()) {
            throw new IllegalArgumentException("El slug no puede estar vacío");
        }
        if (preparationHours < 0) {
            throw new IllegalArgumentException("El tiempo de preparación no puede ser negativo");
        }
        variants = List.copyOf(variants);     // copia inmutable (y falla si la lista es null)
        categories = List.copyOf(categories);
    }

    // Disponible si tiene al menos una variante activa. Si ninguna lo está,
    // la tienda lo muestra como "no disponible por el momento".
    public boolean available() {
        return variants.stream().anyMatch(ProductVariant::active);
    }

    // Lo que tarda una variante de este producto. Es un dato derivado, como available().
    // La regla vive en ProductVariant.effectivePreparationHours.
    public int preparationHoursOf(ProductVariant variant) {
        return ProductVariant.effectivePreparationHours(variant.preparationHours(), preparationHours);
    }
}