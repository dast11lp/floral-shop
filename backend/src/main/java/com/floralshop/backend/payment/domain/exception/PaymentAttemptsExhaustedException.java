package com.floralshop.backend.payment.domain.exception;

// El pedido ya usó todos sus intentos con cobro creado (ADR 0016, A.9; tope en payment_settings).
// El mensaje es el de A.23.
public class PaymentAttemptsExhaustedException extends RuntimeException {

    public PaymentAttemptsExhaustedException() {
        super("Llegaste al máximo de intentos de pago para este pedido. Si quieres, haz un pedido nuevo.");
    }
}
