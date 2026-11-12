package com.floralshop.backend.ordering.infra.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;

// Punta a punta con Postgres real. Sin @Transactional: el caso de uso abre su propia transacción,
// como en producción. Los datos se guardan de verdad y se borran al terminar.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    OrderRepositoryPort orders;

    private final LocalDate inFiveDays = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(5);

    private long zoneId;
    private long variantId;
    private long productId;
    private long slotId;

    @BeforeEach
    void setUp() {
        clean();
        zoneId = jdbc.queryForObject("insert into delivery_zone (name, shipping_fee, travel_margin_hours) "
                + "values ('Zona pedidos', 15000, 1) returning id", Long.class);
        productId = jdbc.queryForObject("insert into product (name, slug, crafting_hours) "
                + "values ('Ramo de rosas', 'ramo-pedidos', 24) returning id", Long.class);
        // El grande ocupa 2 cupos por unidad
        variantId = jdbc.queryForObject("insert into product_variant (product_id, name, price, slot_units) "
                + "values (?, 'Grande', 130000, 2) returning id", Long.class, productId);
        // 4 cupos, dentro de 5 días: siempre alcanza para 2 x 24 h + márgenes
        slotId = jdbc.queryForObject("insert into delivery_slot (slot_date, start_time, end_time, capacity, booked) "
                + "values (?, '09:00', '11:00', 4, 0) returning id", Long.class, inFiveDays);
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from order_line");
        jdbc.update("delete from customer_order");
        jdbc.update("delete from product_variant where name = 'Grande' and product_id in "
                + "(select id from product where slug = 'ramo-pedidos')");
        jdbc.update("delete from product where slug = 'ramo-pedidos'");
        jdbc.update("delete from delivery_zone where name = 'Zona pedidos'");
        jdbc.update("delete from delivery_slot where slot_date = ? and start_time = '09:00'", inFiveDays);
    }

    private ResultActions order(int quantity) throws Exception {
        String body = """
                {"customer": {"name": "Ana", "email": "ana@example.com", "phone": "3000000000"},
                 "deliveryAddress": "Calle 1 # 2-3", "zoneId": %d, "slotId": %d,
                 "lines": [{"variantId": %d, "quantity": %d}]}
                """.formatted(zoneId, slotId, variantId, quantity);
        return mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int booked() {
        return jdbc.queryForObject("select booked from delivery_slot where id = ?", Integer.class, slotId);
    }

    private int orderCount() {
        return jdbc.queryForObject("select count(*) from customer_order", Integer.class);
    }

    @Test
    void createsTheOrderWithItsLinesAndReservesTheSlotUnits() throws Exception {
        order(2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.craftingHours").value(48))
                .andExpect(jsonPath("$.total").value(275000.0));

        assertThat(booked()).isEqualTo(4); // 2 ramos x 2 cupos
        assertThat(orderCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select product_name from order_line", String.class))
                .isEqualTo("Ramo de rosas");
    }

    @Test
    void whenTheSlotHasNoRoomLeftNothingIsSavedAndNothingIsReserved() throws Exception {
        order(2).andExpect(status().isCreated()); // ocupa los 4 cupos

        order(1).andExpect(status().isConflict());

        assertThat(booked()).isEqualTo(4);
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void theOrderKeepsTheEffectiveCraftingTimeWhenTheCatalogChanges() throws Exception {
        // La variante no tiene tiempo propio: hereda las 24 h del producto
        order(1).andExpect(status().isCreated());

        jdbc.update("update product set crafting_hours = 72 where id = ?", productId);
        jdbc.update("update product_variant set crafting_hours = 96 where id = ?", variantId);

        assertThat(jdbc.queryForObject("select crafting_hours from order_line", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForObject("select crafting_hours from customer_order", Integer.class)).isEqualTo(24);
    }

    @Test
    void theOrderKeepsItsPriceWhenTheCatalogChanges() throws Exception {
        order(1).andExpect(status().isCreated());

        jdbc.update("update product_variant set price = 999999 where id = ?", variantId);

        assertThat(jdbc.queryForObject("select unit_price from order_line", java.math.BigDecimal.class))
                .isEqualByComparingTo("130000");
    }

    private double minutesAfterCreation(String column) {
        return jdbc.queryForObject("select extract(epoch from " + column + " - created_at) / 60 from customer_order",
                Double.class);
    }

    // order_settings (V9): 15 minutos para empezar a pagar, con tope de 51.
    @Test
    void aNewOrderHoldsItsSlot15MinutesWithALimitOf51() throws Exception {
        order(1).andExpect(status().isCreated());

        assertThat(minutesAfterCreation("hold_expires_at")).isBetween(14.9, 15.1);
        assertThat(minutesAfterCreation("hold_limit_at")).isBetween(50.9, 51.1);
    }

    // La base vuelve a comprobar el tope: aunque alguien pida pasarse, no actualiza nada.
    @Test
    void theDatabaseNeverMovesTheHoldPastTheLimit() throws Exception {
        order(1).andExpect(status().isCreated());
        long id = jdbc.queryForObject("select id from customer_order", Long.class);
        Instant limit = jdbc.queryForObject("select hold_limit_at from customer_order", java.sql.Timestamp.class)
                .toInstant();

        assertThat(orders.updateHold(id, limit.plusSeconds(60))).isFalse();
        assertThat(minutesAfterCreation("hold_expires_at")).isBetween(14.9, 15.1);

        assertThat(orders.updateHold(id, limit)).isTrue();
        assertThat(minutesAfterCreation("hold_expires_at")).isBetween(50.9, 51.1);
    }

    // Un pedido que ya no está pendiente de pago no extiende su franja.
    @Test
    void aHoldIsOnlyExtendedWhilePendingPayment() throws Exception {
        order(1).andExpect(status().isCreated());
        long id = jdbc.queryForObject("select id from customer_order", Long.class);
        jdbc.update("update customer_order set status = 'CANCELLED' where id = ?", id);

        assertThat(orders.updateHold(id, Instant.now().plusSeconds(20 * 60))).isFalse();
    }
}
