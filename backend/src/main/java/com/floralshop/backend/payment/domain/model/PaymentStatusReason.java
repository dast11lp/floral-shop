package com.floralshop.backend.payment.domain.model;

// Por qué un pago está en su estado, guardado aparte del estado (ADR 0016, D.1 y P5).
// Por ahora, los dos motivos de FAILED (columna status_reason, migración V10).
public enum PaymentStatusReason {
    // La pasarela respondió que no crea el cobro (por ejemplo, datos inválidos).
    GATEWAY_REJECTED,
    // No sabemos si la pasarela creó el cobro: no respondió, o el intento quedó a medio crear (D.3, P6).
    // Se cierra igual, porque nadie puede pagarlo: el enlace nunca llegó al cliente.
    CREATION_UNCONFIRMED
}
