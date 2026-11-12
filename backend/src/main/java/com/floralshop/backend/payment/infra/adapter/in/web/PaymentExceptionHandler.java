package com.floralshop.backend.payment.infra.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.floralshop.backend.ordering.domain.exception.OrderNotFoundException;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.exception.PaymentGatewayException;
import com.floralshop.backend.payment.domain.exception.PaymentOptionNotAvailableException;

// Traduce a HTTP las excepciones que pueden salir al iniciar un pago.
// 404: el pedido no existe.
// 409: el pedido ya no se puede pagar, o ya tiene un pago en curso con otra opción.
// 422: la opción de pago no está disponible.
// 502: la pasarela falló. El detalle técnico va al log; al cliente, un mensaje genérico.
@RestControllerAdvice(basePackageClasses = PaymentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class PaymentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({OrderNotPayableException.class, PaymentAlreadyInProgressException.class})
    ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentOptionNotAvailableException.class)
    ProblemDetail handleUnprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    ProblemDetail handleGateway(PaymentGatewayException ex) {
        log.warn("Falló la pasarela de pago: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "No se pudo iniciar el pago. Intenta de nuevo en unos minutos.");
    }
}
