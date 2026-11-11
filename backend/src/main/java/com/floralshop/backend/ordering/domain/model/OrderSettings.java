package com.floralshop.backend.ordering.domain.model;

// Máximos que edita la florista: unidades por línea y por pedido.
public record OrderSettings(int maxUnitsPerLine, int maxUnitsPerOrder) {

    public OrderSettings {
        if (maxUnitsPerLine <= 0 || maxUnitsPerOrder <= 0) {
            throw new IllegalArgumentException("Los máximos deben ser mayores que cero");
        }
    }
}