package com.floralshop.backend.scheduling.infra.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;
import com.floralshop.backend.scheduling.domain.model.DeliveryOptions;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.SlotOption;

@WebMvcTest(DeliveryOptionsController.class)
class DeliveryOptionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeliveryOptionsService options;

    private static final String VALID = """
            {"zoneId": 7, "lines": [{"variantId": 1, "quantity": 2}]}
            """;

    private org.springframework.test.web.servlet.ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/delivery-options")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void returnsTimeEarliestStartAndSlotsWithoutInternalCounts() throws Exception {
        DeliverySlot slot = new DeliverySlot(3L, LocalDate.of(2026, 12, 16), LocalTime.of(14, 0),
                LocalTime.of(16, 0), 4, 1, true);
        when(options.optionsFor(List.of(new CartLine(1L, 2)), 7L)).thenReturn(new DeliveryOptions(
                48, 1, LocalDateTime.of(2026, 12, 16, 12, 0), List.of(new SlotOption(slot, true))));

        send(VALID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.craftingHours").value(48))
                .andExpect(jsonPath("$.earliestStart").value("2026-12-16T12:00:00"))
                .andExpect(jsonPath("$.slots[0].id").value(3))
                .andExpect(jsonPath("$.slots[0].date").value("2026-12-16"))
                .andExpect(jsonPath("$.slots[0].available").value(true))
                .andExpect(jsonPath("$.slots[0].capacity").doesNotExist())
                .andExpect(jsonPath("$.slots[0].booked").doesNotExist());
    }

    @Test
    void emptyCartIsRejected() throws Exception {
        send("""
                {"zoneId": 7, "lines": []}
                """).andExpect(status().isBadRequest());
        verifyNoInteractions(options);
    }

    @Test
    void missingZoneIsRejected() throws Exception {
        send("""
                {"lines": [{"variantId": 1, "quantity": 1}]}
                """).andExpect(status().isBadRequest());
        verifyNoInteractions(options);
    }

    @Test
    void quantityOutOfRangeIsRejected() throws Exception {
        send("""
                {"zoneId": 7, "lines": [{"variantId": 1, "quantity": 0}]}
                """).andExpect(status().isBadRequest());
        send("""
                {"zoneId": 7, "lines": [{"variantId": 1, "quantity": 101}]}
                """).andExpect(status().isBadRequest());
        verifyNoInteractions(options);
    }

    @Test
    void unknownZoneIs422() throws Exception {
        when(options.optionsFor(anyList(), eq(7L))).thenThrow(new ZoneNotFoundException(7L));

        send(VALID).andExpect(status().isUnprocessableContent());
    }

    @Test
    void unavailableVariantIs422() throws Exception {
        when(options.optionsFor(anyList(), anyLong())).thenThrow(new VariantNotAvailableException(1L));

        send(VALID).andExpect(status().isUnprocessableContent());
    }
}