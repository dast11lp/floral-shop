package com.floralshop.backend.ordering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

// Totales y consistencia del pedido. Java puro.
class OrderModelTest {

    private final CustomerContact customer = new CustomerContact("Ana", "ana@example.com", "3000000000");

    private OrderLine line(String price, int quantity) {
        return new OrderLine(1L, "Ramo", "Mediano", new BigDecimal(price), quantity, 4, 1);
    }

    private static final Instant CREATED = Instant.parse("2026-12-10T15:00:00Z");

    private Order order(String shippingFee, List<OrderLine> lines) {
        return order(shippingFee, lines, CREATED.plusSeconds(15 * 60), CREATED.plusSeconds(51 * 60));
    }

    private Order order(String shippingFee, List<OrderLine> lines, Instant holdExpiresAt, Instant holdLimitAt) {
        return new Order(null, UUID.randomUUID(), OrderStatus.PENDING_PAYMENT, customer, "Calle 1 # 2-3",
                1L, "Centro", new BigDecimal(shippingFee), 9L, LocalDate.of(2026, 12, 16),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 2, lines, holdExpiresAt, holdLimitAt);
    }

    @Test
    void lineTotalIsPriceTimesQuantity() {
        assertThat(line("60000", 3).lineTotal()).isEqualByComparingTo("180000");
    }

    @Test
    void subtotalAddsTheLinesAndTotalAddsShipping() {
        Order order = order("8000", List.of(line("60000", 2), line("90000", 1)));

        assertThat(order.subtotal()).isEqualByComparingTo("210000");
        assertThat(order.total()).isEqualByComparingTo("218000");
    }

    @Test
    void orderWithoutLinesIsRejected() {
        assertThatThrownBy(() -> order("8000", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // La franja puede retenerse justo hasta el tope, pero no más allá.
    @Test
    void holdCanReachTheLimitButNotPassIt() {
        Instant limit = CREATED.plusSeconds(51 * 60);

        assertThat(order("8000", List.of(line("60000", 1)), limit, limit).holdExpiresAt()).isEqualTo(limit);
        assertThatThrownBy(() -> order("8000", List.of(line("60000", 1)), limit.plusSeconds(1), limit))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holdIsRequired() {
        assertThatThrownBy(() -> order("8000", List.of(line("60000", 1)), null, CREATED))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> order("8000", List.of(line("60000", 1)), CREATED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void lineRejectsQuantityNotGreaterThanZero() {
        assertThatThrownBy(() -> line("60000", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customerRejectsBlankData() {
        assertThatThrownBy(() -> new CustomerContact("Ana", " ", "3000000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}