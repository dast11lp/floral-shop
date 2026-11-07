package com.floralshop.backend.catalog.domain.model;

import java.util.List;

// Una página de resultados, más el total para que el cliente sepa cuántas páginas hay.
public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    public PagedResult {
        content = List.copyOf(content); // copia inmutable
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1) {
            throw new IllegalArgumentException("El tamaño debe ser al menos 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("El total no puede ser negativo");
        }
    }

    // Número de páginas: el total dividido por el tamaño, redondeado hacia arriba.
    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}