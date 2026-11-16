package com.floralshop.backend.payment.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.floralshop.backend.payment.domain.model.PaymentStatus;

// La columna es una copia del enum del dominio a propósito. Esta prueba avisa si un estado nuevo del dominio
// no se puede guardar (el adaptador traduce por nombre). El CHECK de la tabla lo prueba DatabaseMigrationTest.
class PaymentStatusColumnTest {

    @Test
    void everyDomainStatusHasAColumnValue() {
        assertThat(Arrays.stream(PaymentStatusColumn.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PaymentStatus.values()).map(Enum::name).toList());
    }
}
