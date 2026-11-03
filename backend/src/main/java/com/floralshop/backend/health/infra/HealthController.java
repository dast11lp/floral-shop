package com.floralshop.backend.health.infra;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Adaptador REST: expone un chequeo de salud mínimo para monitoreo externo.
// No consulta la base de datos ni revela detalles internos (eso lo hace Actuator, solo en la red interna).
@RestController
public class HealthController {

    // Respuesta pública y mínima: solo indica que el proceso está vivo.
    public record HealthResponse(String status) {
    }

    @GetMapping("/api/v1/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}