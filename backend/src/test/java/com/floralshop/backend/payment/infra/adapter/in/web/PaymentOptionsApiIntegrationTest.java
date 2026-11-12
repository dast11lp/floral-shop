package com.floralshop.backend.payment.infra.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.floralshop.backend.TestcontainersConfiguration;

// Punta a punta con Postgres real: la política que crea la migración V7 y el adaptador de Mercado Pago
// con una credencial inventada (solo importa que no esté vacía; aquí no se llama a Mercado Pago).
@SpringBootTest(properties = "payment.mercadopago.access-token=TEST-credencial-inventada")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentOptionsApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    // Arranque de la V7: tarjeta habilitada y sugerida; PSE existe pero está deshabilitado.
    @Test
    void offersMercadoPagoWithCardAsTheSuggestedOption() throws Exception {
        mockMvc.perform(get("/api/v1/payment-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].provider").value("mercadopago"))
                .andExpect(jsonPath("$[0].method").value("CARD"))
                .andExpect(jsonPath("$[0].suggested").value(true));
    }
}
