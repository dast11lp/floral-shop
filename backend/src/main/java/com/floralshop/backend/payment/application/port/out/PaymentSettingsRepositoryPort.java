package com.floralshop.backend.payment.application.port.out;

// Puerto de salida: los ajustes de pagos que se cambian sin tocar el código (tabla payment_settings, una sola fila).
public interface PaymentSettingsRepositoryPort {

    // Cuántos intentos con cobro creado puede tener un pedido (ADR 0016, A.9). Arranca en 5.
    int maxAttemptsPerOrder();
}
