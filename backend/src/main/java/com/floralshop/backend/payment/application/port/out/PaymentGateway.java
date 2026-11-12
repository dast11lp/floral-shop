package com.floralshop.backend.payment.application.port.out;

// Puerto de salida hacia una pasarela de pago (ADR 0003 y 0014). Cada pasarela es un adaptador en infra.
// Crece solo cuando hace falta: crear el cobro llega en el siguiente PR.
public interface PaymentGateway {

    // Identificador de la pasarela, el mismo que se guarda en la política y en cada pago ("mercadopago").
    String provider();

    // Si el servidor tiene sus credenciales. Sin ellas, sus combinaciones no se ofrecen (ADR 0014, punto 2).
    boolean isConfigured();
}
