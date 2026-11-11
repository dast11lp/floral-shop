package com.floralshop.backend.catalog.domain.model;

// Lo que pide un carrito a la florista: cuántas horas de trabajo y cuántos cupos de una franja.
public record CartWorkload(int craftingHours, int slotUnits) {

    public CartWorkload {
        if (craftingHours < 0) {
            throw new IllegalArgumentException("El tiempo de elaboración no puede ser negativo");
        }
        if (slotUnits <= 0) {
            throw new IllegalArgumentException("Un carrito ocupa al menos un cupo");
        }
    }
}