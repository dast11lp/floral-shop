package com.floralshop.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Máquina de estados del pago (ADR 0003). Java puro.
class PaymentStatusTest {

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void pendingCanMoveToAnyOtherState(PaymentStatus next) {
        assertThat(PaymentStatus.PENDING.canTransitionTo(next)).isTrue();
    }

    @Test
    void pendingCannotMoveToItself() {
        assertThat(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.PENDING)).isFalse();
    }

    // Los estados finales no tienen salidas, ni siquiera hacia sí mismos.
    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void finalStatesHaveNoExits(PaymentStatus from) {
        for (PaymentStatus next : PaymentStatus.values()) {
            assertThat(from.canTransitionTo(next))
                    .as("%s -> %s", from, next)
                    .isFalse();
        }
    }
}