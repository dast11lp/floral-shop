package com.floralshop.backend.ordering.infra.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.ordering.domain.exception.OrderLimitExceededException;
import com.floralshop.backend.scheduling.domain.exception.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.exception.SlotNotFoundException;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;

// Traduce a HTTP las excepciones de dominio que pueden salir al crear un pedido.
// 409: la franja ya no está disponible (otro cliente tomó el cupo o ya no alcanza el tiempo);
//      el cliente debe elegir otra.
// 422: la petición está bien formada, pero apunta a algo que no se puede usar o supera los máximos.
@RestControllerAdvice(basePackageClasses = OrderController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class OrderingExceptionHandler {

    @ExceptionHandler({SlotNotAvailableException.class, SlotNotFoundException.class})
    ProblemDetail handleSlotNotAvailable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ZoneNotFoundException.class, VariantNotAvailableException.class,
            OrderLimitExceededException.class})
    ProblemDetail handleUnprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }
}