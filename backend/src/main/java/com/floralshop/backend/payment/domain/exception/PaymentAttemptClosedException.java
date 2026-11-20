package com.floralshop.backend.payment.domain.exception;

// El intento ya lo cerró otro proceso mientras se esperaba a la pasarela (ADR 0016, D.8).
// El enlace de pago no se guardó y no se entrega: nadie puede pagar ese cobro, que vence solo (D.3).
// El mensaje no lleva el enlace. El front lleva al cliente a "Tu pedido", que muestra el estado real
// (pendiente 52, punto 15).
public class PaymentAttemptClosedException extends RuntimeException {

    public PaymentAttemptClosedException() {
        super("Este intento de pago ya se cerró. Revisa el estado de tu pedido.");
    }
}
