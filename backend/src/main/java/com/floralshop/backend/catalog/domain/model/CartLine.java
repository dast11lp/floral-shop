package com.floralshop.backend.catalog.domain.model;

// Una línea del carrito: qué variante y cuántas unidades.
public record CartLine(long variantId, int quantity) {

    public CartLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}