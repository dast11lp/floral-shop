package com.floralshop.backend.shared.infra.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Manejador global: da formato (ProblemDetail) a los errores estándar de Spring, como parámetros
// inválidos (400), rutas inexistentes (404) o métodos no permitidos (405), y atrapa lo imprevisto.
// Lo extienden todos los controladores. Va de último, para que los de cada módulo tengan prioridad.
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Último recurso: cualquier error no previsto. El detalle completo queda en el log,
    // y el cliente recibe un mensaje genérico, sin pistas sobre el interior del sistema.
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}