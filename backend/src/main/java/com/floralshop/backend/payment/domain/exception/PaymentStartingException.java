package com.floralshop.backend.payment.domain.exception;

// El pedido tiene un intento sin enlace todavía dentro del plazo de gracia: se está creando el cobro
// (ADR 0016, D.3). Pasado el plazo, el intento se cierra y se puede volver a intentar.
public class PaymentStartingException extends RuntimeException {

    public PaymentStartingException() {
        super("Tu pago se está iniciando; espera un momento.");
    }
}
