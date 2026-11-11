package com.floralshop.backend.scheduling.domain.model;

// Ajustes de entrega que edita la florista. Por ahora solo el margen de despacho:
// las horas que se suman a toda entrega para empacar, salir e imprevistos (ADR 0012, punto 5).
public record DeliverySettings(int dispatchMarginHours) {

    public DeliverySettings {
        if (dispatchMarginHours < 0) {
            throw new IllegalArgumentException("El margen de despacho no puede ser negativo");
        }
    }
}