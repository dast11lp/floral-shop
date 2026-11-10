package com.floralshop.backend.scheduling.infra.adapter.in.web;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

@WebMvcTest(SchedulingController.class)
class SchedulingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SchedulingQueryService scheduling;

    private DeliverySlot slot(long id, int capacity, int booked) {
        return new DeliverySlot(id, LocalDate.of(2026, 12, 10), LocalTime.of(9, 0), LocalTime.of(11, 0),
                capacity, booked, true);
    }

    @Test
    void listsSlotsWithAvailabilityAndWithoutCounts() throws Exception {
        when(scheduling.listSlots()).thenReturn(List.of(slot(1L, 4, 1), slot(2L, 4, 4)));

        mockMvc.perform(get("/api/v1/delivery-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].date").value("2026-12-10"))
                .andExpect(jsonPath("$[0].startTime").value(startsWith("09:00")))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].available").value(false)) // llena
                // Las cifras no se exponen:
                .andExpect(jsonPath("$[0].capacity").doesNotExist())
                .andExpect(jsonPath("$[0].booked").doesNotExist());
    }

    @Test
    void listsZones() throws Exception {
        when(scheduling.listZones()).thenReturn(List.of(new DeliveryZone(1L, "Centro", new BigDecimal("8000"), true)));

        mockMvc.perform(get("/api/v1/delivery-zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Centro"))
                .andExpect(jsonPath("$[0].shippingFee").value(8000))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }
}