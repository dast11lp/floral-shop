package com.floralshop.backend.catalog.domain.model;

// Qué página se pide. Las páginas empiezan en 0.
public record PageQuery(int page, int size) {

    // Tope de elementos por página, para que nadie pida el catálogo entero de un golpe.
    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El tamaño debe estar entre 1 y " + MAX_SIZE);
        }
    }
}