package com.floralshop.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Qué medios no pasan por una pasarela (ADR 0003, punto 3). Java puro.
class PaymentMethodTest {

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"MANUAL_TRANSFER", "CASH_ON_DELIVERY"})
    void manualMethodsAreManual(PaymentMethod method) {
        assertThat(method.isManual()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"MANUAL_TRANSFER", "CASH_ON_DELIVERY"},
            mode = EnumSource.Mode.EXCLUDE)
    void onlineMethodsAreNotManual(PaymentMethod method) {
        assertThat(method.isManual()).isFalse();
    }
}