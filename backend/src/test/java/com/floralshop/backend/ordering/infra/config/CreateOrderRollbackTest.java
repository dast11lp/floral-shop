package com.floralshop.backend.ordering.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.application.service.CreateOrderService;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.NewOrder;

// ADR 0013, punto 1: si guardar el pedido falla después de apartar el cupo, el cupo también se deshace.
// Se fuerza el fallo con un espía sobre el puerto de pedidos. Sin @Transactional en la prueba:
// la transacción debe abrirla el caso de uso.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CreateOrderRollbackTest {

    @Autowired
    CreateOrderService createOrder;

    @MockitoSpyBean
    OrderRepositoryPort orders;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate inSixDays = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(6);
    private long zoneId;
    private long variantId;
    private long slotId;

    @BeforeEach
    void setUp() {
        zoneId = jdbc.queryForObject("insert into delivery_zone (name, shipping_fee) "
                + "values ('Zona rollback', 8000) returning id", Long.class);
        long productId = jdbc.queryForObject("insert into product (name, slug, crafting_hours) "
                + "values ('Ramo', 'ramo-rollback', 4) returning id", Long.class);
        variantId = jdbc.queryForObject("insert into product_variant (product_id, name, price) "
                + "values (?, 'Único', 60000) returning id", Long.class, productId);
        slotId = jdbc.queryForObject("insert into delivery_slot (slot_date, start_time, end_time, capacity, booked) "
                + "values (?, '14:00', '16:00', 4, 0) returning id", Long.class, inSixDays);
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from order_line");
        jdbc.update("delete from customer_order");
        jdbc.update("delete from product_variant where id = ?", variantId);
        jdbc.update("delete from product where slug = 'ramo-rollback'");
        jdbc.update("delete from delivery_zone where id = ?", zoneId);
        jdbc.update("delete from delivery_slot where id = ?", slotId);
    }

    @Test
    void ifSavingTheOrderFailsTheReservedUnitsAreReleased() {
        doThrow(new IllegalStateException("La base de datos falló al guardar")).when(orders).save(any());
        NewOrder request = new NewOrder(new CustomerContact("Ana", "ana@example.com", "3000000000"),
                "Calle 1 # 2-3", zoneId, slotId, List.of(new CartLine(variantId, 2)));

        // Spring envuelve los errores de un @Repository en su propia excepción: se comprueba el mensaje.
        assertThatThrownBy(() -> createOrder.create(request)).hasMessageContaining("falló al guardar");

        assertThat(jdbc.queryForObject("select booked from delivery_slot where id = ?", Integer.class, slotId))
                .isZero();
    }
}