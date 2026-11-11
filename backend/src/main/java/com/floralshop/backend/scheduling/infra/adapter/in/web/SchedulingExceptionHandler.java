package com.floralshop.backend.scheduling.infra.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;

// Traduce a HTTP las excepciones de dominio que pueden salir de los controladores de scheduling.
// 422: la petición está bien formada, pero apunta a una zona o una variante que no se pueden usar.
// Los errores de formato y validación (400) y lo imprevisto (500) los maneja el GlobalExceptionHandler.
@RestControllerAdvice(basePackageClasses = SchedulingController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SchedulingExceptionHandler {

    @ExceptionHandler(ZoneNotFoundException.class)
    ProblemDetail handleZoneNotFound(ZoneNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(VariantNotAvailableException.class)
    ProblemDetail handleVariantNotAvailable(VariantNotAvailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }
}