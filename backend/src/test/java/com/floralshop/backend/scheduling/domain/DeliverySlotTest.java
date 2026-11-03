package com.floralshop.backend.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

// Prueba de Java puro: sin Spring ni base de datos. Corre en milisegundos.
class DeliverySlotTest {

    // Crea una franja válida de 9:00 a 11:00 con la capacidad y lo reservado que le pidas.
    private DeliverySlot slot(int capacity, int booked) {
        return new DeliverySlot(null, LocalDate.of(2026, 12, 24),
                LocalTime.of(9, 0), LocalTime.of(11, 0), capacity, booked, true);
    }

    @Test
    void availableIsCapacityMinusBooked() {
        assertThat(slot(10, 3).available()).isEqualTo(7);
    }

    @Test
    void acceptsFullSlot() {
        // booked == capacity es válido: la franja está llena, no inválida
        assertThat(slot(4, 4).available()).isZero();
    }

    @Test
    void rejectsBookedGreaterThanCapacity() {
        assertThatThrownBy(() -> slot(2, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeBooked() {
        assertThatThrownBy(() -> slot(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeCapacity() {
        assertThatThrownBy(() -> slot(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEndTimeNotAfterStartTime() {
        assertThatThrownBy(() -> new DeliverySlot(null, LocalDate.of(2026, 12, 24),
                LocalTime.of(11, 0), LocalTime.of(9, 0), 10, 0, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}