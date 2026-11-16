package com.floralshop.backend.payment.domain.model;

import static com.floralshop.backend.payment.domain.model.PaymentStatus.*;
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

// Máquina de estados del pago, copiada de la tabla del ADR 0016 (sección B). Java puro.
// Se prueba cada par origen -> destino: los permitidos y todos los demás.
class PaymentStatusTest {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(PAID, FAILED, EXPIRED, CANCELLED),
            PAID, EnumSet.of(REFUNDED),     // A.4
            FAILED, EnumSet.of(PAID),       // A.21 (con referencia: lo revisa Payment)
            EXPIRED, EnumSet.of(PAID),      // A.21
            CANCELLED, EnumSet.of(PAID),    // A.21
            REFUNDED, EnumSet.noneOf(PaymentStatus.class));

    static Stream<Arguments> everyPair() {
        return Arrays.stream(values()).flatMap(from -> Arrays.stream(values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("everyPair")
    void onlyTheTransitionsOfTheAdrAreAllowed(PaymentStatus from, PaymentStatus to) {
        assertThat(from.canTransitionTo(to)).isEqualTo(ALLOWED.get(from).contains(to));
    }

    // La tabla de la prueba cubre todos los estados: uno nuevo sin reglas hace fallar la prueba.
    @Test
    void everyStatusHasItsRules() {
        assertThat(ALLOWED.keySet()).containsExactlyInAnyOrder(values());
    }

    // Un reembolso de un pago todavía pendiente no lo cambia (ADR 0016, D.4, provisional hasta P9).
    @Test
    void aPendingPaymentCannotBeRefunded() {
        assertThat(PENDING.canTransitionTo(REFUNDED)).isFalse();
    }

    @Test
    void theTargetIsRequired() {
        assertThatThrownBy(() -> PENDING.canTransitionTo(null)).isInstanceOf(NullPointerException.class);
    }
}
