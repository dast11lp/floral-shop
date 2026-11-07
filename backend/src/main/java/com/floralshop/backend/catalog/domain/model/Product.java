package com.floralshop.backend.catalog.domain.model;

import java.util.List;
import java.util.Objects;

// Producto del catálogo, con sus variantes y sus categorías.
// Es inmutable: las listas se copian al crear el objeto y nadie puede modificarlas después.
public record Product(Long id, String name, String slug, String description, boolean active,
                      List<ProductVariant> variants, List<Category> categories) {

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
        variants = List.copyOf(variants);     // copia inmutable (y falla si la lista es null)
        categories = List.copyOf(categories);
    }
}