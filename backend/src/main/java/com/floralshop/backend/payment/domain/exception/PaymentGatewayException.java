package com.floralshop.backend.payment.domain.exception;

// La pasarela no respondió o rechazó la petición. El mensaje no lleva credenciales ni datos del cliente.
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
