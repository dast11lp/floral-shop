package com.floralshop.backend.catalog.infra.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.floralshop.backend.catalog.domain.exception.ProductNotFoundException;

// Traduce las excepciones de dominio del módulo catalog a respuestas HTTP.
// Los errores estándar y el último recurso los maneja el GlobalExceptionHandler (shared).
// Va con máxima prioridad para que el global no se las adelante.
@RestControllerAdvice(basePackageClasses = CatalogController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class CatalogExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}