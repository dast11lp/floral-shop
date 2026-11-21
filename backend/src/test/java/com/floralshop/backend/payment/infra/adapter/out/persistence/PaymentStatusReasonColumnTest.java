package com.floralshop.backend.payment.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// La columna es una copia del enum del dominio a propósito. Esta prueba avisa si un motivo nuevo del dominio
// no se puede guardar (el adaptador traduce por nombre). El CHECK de la tabla lo prueba DatabaseMigrationTest.
class PaymentStatusReasonColumnTest {

    @Test
    void everyDomainReasonHasAColumnValue() {
        assertThat(Arrays.stream(PaymentStatusReasonColumn.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(PaymentStatusReason.values()).map(Enum::name).toList());
    }
}
