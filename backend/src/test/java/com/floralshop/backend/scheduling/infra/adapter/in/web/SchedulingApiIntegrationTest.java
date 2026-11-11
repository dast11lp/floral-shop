package com.floralshop.backend.scheduling.infra.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.TestcontainersConfiguration;

// Punta a punta con Postgres real. @Transactional: se deshace solo (estas lecturas no necesitan
// una transacción propia, a diferencia de las del catálogo, que cargan colecciones perezosas).
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SchedulingApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));

    private void slot(LocalDate date, boolean open) {
        jdbc.update("insert into delivery_slot (slot_date, start_time, end_time, capacity, booked, is_open) "
                + "values (?, '09:00', '11:00', 4, 0, ?)", date, open);
    }

    @Test
    void showsOpenSlotsFromTodayUpToTheHorizonOnly() throws Exception {
        slot(today.minusDays(1), true);  // ayer: no sale
        slot(today.plusDays(2), false);  // cerrada: no sale
        slot(today.plusDays(30), true);  // último día del horizonte: sale
        slot(today.plusDays(31), true);  // más allá del horizonte: no sale
        slot(today.plusDays(1), true);   // mañana: sale

        mockMvc.perform(get("/api/v1/delivery-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value(today.plusDays(1).toString()))
                .andExpect(jsonPath("$[1].date").value(today.plusDays(30).toString()));
    }

    // ---- POST /delivery-options, punta a punta ----

    private long zone(String name, int travelMarginHours) {
        return jdbc.queryForObject("insert into delivery_zone (name, shipping_fee, travel_margin_hours) "
                + "values (?, 8000, ?) returning id", Long.class, name, travelMarginHours);
    }

    private long variant(int productHours, Integer variantHours, int slotUnits) {
        long product = jdbc.queryForObject("insert into product (name, slug, crafting_hours) "
                + "values ('Ramo', 'ramo-opciones', ?) returning id", Long.class, productHours);
        return jdbc.queryForObject("insert into product_variant (product_id, name, price, crafting_hours, slot_units) "
                + "values (?, 'Grande', 90000, ?, ?) returning id", Long.class, product, variantHours, slotUnits);
    }

    @Test
    void deliveryOptionsMarkTheSlotsTheCartCanTake() throws Exception {
        // 2 ramos que heredan 24 h = 48 h, más 2 h generales y 1 h de la zona = 51 h desde ahora.
        // Mañana no alcanza; dentro de 3 días a las 9:00 siempre alcanza (72 h o más).
        long zone = zone("Chía", 1);
        long variant = variant(24, null, 1);
        slot(today.plusDays(1), true);
        slot(today.plusDays(3), true);

        mockMvc.perform(post("/api/v1/delivery-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\": " + zone + ", \"lines\": [{\"variantId\": " + variant
                                + ", \"quantity\": 2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.craftingHours").value(48))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[0].available").value(false))
                .andExpect(jsonPath("$.slots[1].available").value(true));
    }

    @Test
    void deliveryOptionsRejectAnUnknownZone() throws Exception {
        long variant = variant(4, null, 1);

        mockMvc.perform(post("/api/v1/delivery-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\": 999999, \"lines\": [{\"variantId\": " + variant
                                + ", \"quantity\": 1}]}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void listsOnlyActiveZones() throws Exception {
        jdbc.update("insert into delivery_zone (name, shipping_fee, active) values (?, ?, ?)",
                "Centro", new BigDecimal("8000"), true);
        jdbc.update("insert into delivery_zone (name, shipping_fee, active) values (?, ?, ?)",
                "Antigua", new BigDecimal("15000"), false);

        mockMvc.perform(get("/api/v1/delivery-zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Centro"));
    }
}