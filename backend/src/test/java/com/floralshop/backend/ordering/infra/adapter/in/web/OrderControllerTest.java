package com.floralshop.backend.ordering.infra.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.ordering.application.service.CreateOrderService;
import com.floralshop.backend.ordering.domain.exception.OrderLimitExceededException;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.scheduling.domain.exception.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CreateOrderService createOrder;

    private static final String VALID = """
            {"customer": {"name": "Ana", "email": "ana@example.com", "phone": "3000000000"},
             "deliveryAddress": "Calle 1 # 2-3", "zoneId": 7, "slotId": 9,
             "lines": [{"variantId": 1, "quantity": 2}]}
            """;

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private final UUID code = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Order order() {
        return new Order(55L, code, OrderStatus.PENDING_PAYMENT,
                new CustomerContact("Ana", "ana@example.com", "3000000000"), "Calle 1 # 2-3",
                7L, "Chía", new BigDecimal("15000"), 9L, LocalDate.of(2026, 12, 18),
                LocalTime.of(9, 0), LocalTime.of(11, 0), 96, 4,
                List.of(new OrderLine(1L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 2, 48, 2)));
    }

    @Test
    void createsTheOrderAndShowsOnlyWhatTheCustomerNeeds() throws Exception {
        when(createOrder.create(any())).thenReturn(order());

        send(VALID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.slotDate").value("2026-12-18"))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(260000))
                .andExpect(jsonPath("$.subtotal").value(260000))
                .andExpect(jsonPath("$.total").value(275000))
                // Ni el id interno ni los datos personales:
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customer").doesNotExist())
                .andExpect(jsonPath("$.deliveryAddress").doesNotExist());
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        send(VALID.replace("ana@example.com", "no-es-correo")).andExpect(status().isBadRequest());
        verifyNoInteractions(createOrder);
    }

    @Test
    void missingSlotIsRejected() throws Exception {
        send(VALID.replace("\"slotId\": 9,", "")).andExpect(status().isBadRequest());
        verifyNoInteractions(createOrder);
    }

    @Test
    void slotNoLongerAvailableIs409() throws Exception {
        when(createOrder.create(any())).thenThrow(new SlotNotAvailableException("No hay cupo suficiente"));

        send(VALID).andExpect(status().isConflict());
    }

    @Test
    void unknownZoneUnavailableVariantOrOverTheLimitIs422() throws Exception {
        when(createOrder.create(any()))
                .thenThrow(new ZoneNotFoundException(7L))
                .thenThrow(new VariantNotAvailableException(1L))
                .thenThrow(new OrderLimitExceededException("Máximo 20 unidades por pedido"));

        send(VALID).andExpect(status().isUnprocessableContent());
        send(VALID).andExpect(status().isUnprocessableContent());
        send(VALID).andExpect(status().isUnprocessableContent());
    }
}