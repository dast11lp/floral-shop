package com.floralshop.backend.catalog.domain.model;

// Lo que pide un carrito a la florista: cuántas horas de trabajo y cuántos cupos de una franja.
public record CartPreparation(int preparationHours, int slotUnits) {

    public CartPreparation {
        if (preparationHours < 0) {
            throw new IllegalArgumentException("El tiempo de preparación no puede ser negativo");
        }
        if (slotUnits <= 0) {
            throw new IllegalArgumentException("Un carrito ocupa al menos un cupo");
        }
    }
}