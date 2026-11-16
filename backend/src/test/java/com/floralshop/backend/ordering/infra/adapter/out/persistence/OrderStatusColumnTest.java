package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.floralshop.backend.ordering.domain.model.OrderStatus;

// La columna es una copia del enum del dominio a propósito. Esta prueba avisa si un estado nuevo del dominio
// no se puede guardar (el mapper traduce por nombre). El CHECK de la tabla lo prueba DatabaseMigrationTest.
class OrderStatusColumnTest {

    @Test
    void everyDomainStatusHasAColumnValue() {
        assertThat(Arrays.stream(OrderStatusColumn.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(OrderStatus.values()).map(Enum::name).toList());
    }
}
