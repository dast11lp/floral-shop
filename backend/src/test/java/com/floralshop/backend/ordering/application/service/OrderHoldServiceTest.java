package com.floralshop.backend.ordering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;

// Extender la retención de la franja: hasta lo pedido, nunca más allá del tope y nunca para acortarla.
// Un pedido creado a las 15:00 vence a las 15:15 y tiene tope a las 15:51 (los 15 y 51 minutos de V9).
@ExtendWith(MockitoExtension.class)
class OrderHoldServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-12T15:00:00Z");
    private static final Instant EXPIRES = CREATED.plusSeconds(15 * 60);
    private static final Instant LIMIT = CREATED.plusSeconds(51 * 60);

    @Mock OrderRepositoryPort orders;

    private OrderHoldService service;

    @BeforeEach
    void setUp() {
        service = new OrderHoldService(orders);
    }

    private static Order order(OrderStatus status) {
        return new Order(42L, UUID.randomUUID(), status, new CustomerContact("Ana", "ana@example.com", "3000000000"),
                "Calle 1 # 2-3", 1L, "Chapinero", new BigDecimal("15000"), 9L, LocalDate.of(2026, 11, 20),
                LocalTime.of(9, 0), LocalTime.of(11, 0), 24, 2,
                List.of(new OrderLine(5L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 1, 24, 2)),
                EXPIRES, LIMIT);
    }

    // Pago iniciado a las 15:05 con 36 minutos: vence a las 15:41, antes del tope.
    @Test
    void extendsToTheRequestedTime() {
        Instant requested = CREATED.plusSeconds(41 * 60);
        when(orders.updateHold(42L, requested)).thenReturn(true);

        assertThat(service.extendHold(order(OrderStatus.PENDING_PAYMENT), requested)).isEqualTo(requested);
    }

    // Pago iniciado a las 15:20 con 36 minutos pediría las 15:56: se queda en el tope, 15:51.
    @Test
    void neverGoesPastTheLimit() {
        when(orders.updateHold(42L, LIMIT)).thenReturn(true);

        assertThat(service.extendHold(order(OrderStatus.PENDING_PAYMENT), CREATED.plusSeconds(56 * 60)))
                .isEqualTo(LIMIT);
        verify(orders).updateHold(42L, LIMIT);
    }

    @Test
    void neverShortensTheHold() {
        assertThat(service.extendHold(order(OrderStatus.PENDING_PAYMENT), CREATED.plusSeconds(10 * 60)))
                .isEqualTo(EXPIRES);
        assertThat(service.extendHold(order(OrderStatus.PENDING_PAYMENT), EXPIRES)).isEqualTo(EXPIRES);
        verifyNoInteractions(orders);
    }

    @Test
    void anOrderThatIsNoLongerPendingIsNotTouched() {
        assertThat(service.extendHold(order(OrderStatus.CONFIRMED), CREATED.plusSeconds(41 * 60)))
                .isEqualTo(EXPIRES);
        verifyNoInteractions(orders);
    }

    // La base no actualizó (el pedido dejó de estar pendiente entre la lectura y la escritura):
    // se informa el vencimiento que había.
    @Test
    void ifTheDatabaseDoesNotUpdateTheHoldStaysAsItWas() {
        when(orders.updateHold(anyLong(), any())).thenReturn(false);

        assertThat(service.extendHold(order(OrderStatus.PENDING_PAYMENT), CREATED.plusSeconds(41 * 60)))
                .isEqualTo(EXPIRES);
    }
}
