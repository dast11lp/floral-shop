package com.floralshop.backend.catalog.domain.model;

import java.util.Objects;

// Categoría de productos. Es plana: no hay subcategorías (pendiente).
public record Category(Long id, String name, String slug, boolean active, int sortOrder) {

    public Category {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(slug, "El slug es obligatorio");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (slug.isBlank()) {
            throw new IllegalArgumentException("El slug no puede estar vacío");
        }
    }
}