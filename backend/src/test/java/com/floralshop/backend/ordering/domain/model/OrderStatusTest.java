package com.floralshop.backend.ordering.domain.model;

import static com.floralshop.backend.ordering.domain.model.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// Máquina de estados del pedido, copiada de la tabla del ADR 0016 (sección C). Java puro.
// Se prueba cada par origen -> destino: los permitidos y todos los demás.
class OrderStatusTest {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            PENDING_PAYMENT, EnumSet.of(CONFIRMED, PENDING_PAYMENT_NO_SLOT, CANCELLED), // A.1, A.2, A.12
            PENDING_PAYMENT_NO_SLOT, EnumSet.of(PAID_WITHOUT_SLOT, CANCELLED),          // A.3, A.16
            CONFIRMED, EnumSet.noneOf(OrderStatus.class),          // sin salidas en el ADR 0016
            CANCELLED, EnumSet.noneOf(OrderStatus.class),          // final (A.6)
            PAID_WITHOUT_SLOT, EnumSet.noneOf(OrderStatus.class)); // gestión de la florista: pendiente (P10)

    static Stream<Arguments> everyPair() {
        return Arrays.stream(values()).flatMap(from -> Arrays.stream(values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("everyPair")
    void onlyTheTransitionsOfTheAdrAreAllowed(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isEqualTo(ALLOWED.get(from).contains(to));
    }

    // La tabla de la prueba cubre todos los estados: uno nuevo sin reglas hace fallar la prueba.
    @Test
    void everyStatusHasItsRules() {
        assertThat(ALLOWED.keySet()).containsExactlyInAnyOrder(values());
    }

    // Un pedido sin franja no recupera la franja ni pasa a confirmado (A.3: la franja no se reasigna).
    @Test
    void anOrderWithoutSlotNeverGetsConfirmed() {
        assertThat(PENDING_PAYMENT_NO_SLOT.canTransitionTo(CONFIRMED)).isFalse();
        assertThat(PENDING_PAYMENT_NO_SLOT.canTransitionTo(PENDING_PAYMENT)).isFalse();
    }

    @Test
    void theTargetIsRequired() {
        assertThatThrownBy(() -> PENDING_PAYMENT.canTransitionTo(null)).isInstanceOf(NullPointerException.class);
    }
}
