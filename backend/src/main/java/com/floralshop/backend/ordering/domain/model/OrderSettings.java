package com.floralshop.backend.ordering.domain.model;

// Ajustes de pedidos que edita la florista.
// maxUnitsPerLine y maxUnitsPerOrder: máximos de unidades por línea y por pedido.
// paymentStartMinutes: minutos para empezar a pagar desde que se crea el pedido (la franja se retiene ese tiempo).
// maxHoldMinutes: tope total de retención de la franja, contado desde que se crea el pedido.
public record OrderSettings(int maxUnitsPerLine, int maxUnitsPerOrder, int paymentStartMinutes, int maxHoldMinutes) {

    public OrderSettings {
        if (maxUnitsPerLine <= 0 || maxUnitsPerOrder <= 0) {
            throw new IllegalArgumentException("Los máximos deben ser mayores que cero");
        }
        if (paymentStartMinutes <= 0 || maxHoldMinutes < paymentStartMinutes) {
            throw new IllegalArgumentException("Los minutos para pagar deben ser positivos y no pasar del tope");
        }
    }
}
