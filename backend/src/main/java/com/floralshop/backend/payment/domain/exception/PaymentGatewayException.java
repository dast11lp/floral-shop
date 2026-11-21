package com.floralshop.backend.payment.domain.exception;

// La pasarela no respondió o rechazó la petición. El mensaje no lleva credenciales ni datos del cliente.
// rejected: true solo si la pasarela respondió CLARAMENTE que la petición está mal o no está autorizada:
// sabemos que no se creó. Qué respuestas cuentan lo decide el adaptador de cada pasarela y endpoint.
// false si no sabemos qué pasó allá (no respondió, se agotó el tiempo, un 5xx, un 4xx dudoso como 409, 423
// o 429, o una respuesta incompleta).
// Con eso se guarda el motivo del intento fallido (ADR 0016, P5: GATEWAY_REJECTED o CREATION_UNCONFIRMED).
public class PaymentGatewayException extends RuntimeException {

    private final boolean rejected;

    public PaymentGatewayException(String message, Throwable cause, boolean rejected) {
        super(message, cause);
        this.rejected = rejected;
    }

    // Sin saber qué pasó allá: el caso por defecto, el más prudente.
    public PaymentGatewayException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public boolean rejected() {
        return rejected;
    }
}
