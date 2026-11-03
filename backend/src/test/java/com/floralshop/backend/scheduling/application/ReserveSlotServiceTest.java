package com.floralshop.backend.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.scheduling.domain.DeliverySlot;
import com.floralshop.backend.scheduling.domain.DeliverySlotRepository;
import com.floralshop.backend.scheduling.domain.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.SlotNotFoundException;

// Prueba unitaria con Mockito: sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class) // activa Mockito en esta clase
class ReserveSlotServiceTest {

    @Mock
    DeliverySlotRepository slots; // doble del puerto: devuelve lo que le indiquemos

    ReserveSlotService service;

    @BeforeEach
    void setUp() {
        service = new ReserveSlotService(slots); // se crea de cero antes de cada prueba
    }

    // Franja válida de 9:00 a 11:00 con los valores que le pidas.
    private DeliverySlot slot(int capacity, int booked, boolean open) {
        return new DeliverySlot(1L, LocalDate.of(2026, 12, 24),
                LocalTime.of(9, 0), LocalTime.of(11, 0), capacity, booked, open);
    }

    @Test
    void reservesUnitsAndSavesUpdatedSlot() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(10, 3, true)));
        when(slots.save(any(DeliverySlot.class))).thenAnswer(inv -> inv.getArgument(0)); // devuelve lo que recibe

        DeliverySlot result = service.reserve(1L, 2);

        assertThat(result.booked()).isEqualTo(5);

        ArgumentCaptor<DeliverySlot> saved = ArgumentCaptor.forClass(DeliverySlot.class);
        verify(slots).save(saved.capture()); // captura lo que el servicio mandó a guardar
        assertThat(saved.getValue().booked()).isEqualTo(5);
    }

    @Test
    void canReserveExactlyTheRemainingCapacity() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(10, 8, true)));
        when(slots.save(any(DeliverySlot.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliverySlot result = service.reserve(1L, 2); // quedaban 2: el límite

        assertThat(result.available()).isZero();
    }

    @Test
    void failsWhenSlotDoesNotExist() {
        when(slots.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(99L, 1))
                .isInstanceOf(SlotNotFoundException.class);
        verify(slots, never()).save(any()); // si falla, no se guarda nada
    }

    @Test
    void failsWhenSlotIsClosed() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(10, 0, false)));

        assertThatThrownBy(() -> service.reserve(1L, 1))
                .isInstanceOf(SlotNotAvailableException.class);
        verify(slots, never()).save(any());
    }

    @Test
    void failsWhenNotEnoughCapacity() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(10, 9, true)));

        assertThatThrownBy(() -> service.reserve(1L, 2)) // queda 1 y se piden 2
                .isInstanceOf(SlotNotAvailableException.class);
        verify(slots, never()).save(any());
    }

    @Test
    void failsWhenUnitsAreNotPositive() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(10, 0, true)));

        assertThatThrownBy(() -> service.reserve(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verify(slots, never()).save(any());
    }
}