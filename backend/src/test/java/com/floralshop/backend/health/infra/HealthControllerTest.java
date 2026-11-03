package com.floralshop.backend.health.infra;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

// Indica que se realizará una prueba enfocada en HealthController.
// Es una alternativa más ligera a @SpringBootTest.
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    // Spring inyecta MockMvc para simular peticiones HTTP
    // sin iniciar un servidor web real ni utilizar un puerto.
    @Autowired
    MockMvc mockMvc;

    @Test
    void healthReturnsUp() throws Exception {

        // Simula una petición GET al endpoint /api/v1/health.
        mockMvc.perform(get("/api/v1/health"))

                // Comprueba que el servidor responda con HTTP 200 (OK).
                .andExpect(status().isOk())

                // Comprueba que el JSON de respuesta contenga
                // una propiedad llamada "status" con el valor "UP". 
                // en JSONPath, "$" representa el objeto raíz de la respuesta JSON.
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
