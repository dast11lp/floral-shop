package com.floralshop.backend.payment.domain.exception;

// El pedido ya tiene un pago en curso: con otra opción, o con la misma pero todavía creándose.
// Un pedido nunca tiene dos pagos pendientes (ADR 0014). La espera está acotada por el tope de la franja del pedido.
public class PaymentAlreadyInProgressException extends RuntimeException {

    public PaymentAlreadyInProgressException() {
        super("Este pedido ya tiene un pago en curso. Termina ese pago, o espera unos minutos a que venza "
                + "para intentar con otra opción.");
    }
}
