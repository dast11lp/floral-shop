package com.floralshop.backend.scheduling.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.catalog.application.service.CartWorkloadService;
import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.CartWorkload;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;
import com.floralshop.backend.scheduling.domain.model.DeliveryOptions;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.SlotOption;

// Qué franjas alcanzan para un carrito: a tiempo, abiertas y con cupos suficientes.
@ExtendWith(MockitoExtension.class)
class DeliveryOptionsServiceTest {

    @Mock
    CartWorkloadService cart;

    @Mock
    DeliveryTimeService deliveryTime;

    @Mock
    SchedulingQueryService scheduling;

    DeliveryOptionsService service;

    private final List<CartLine> lines = List.of(new CartLine(1L, 1));
    // El carrito puede entregarse desde el miércoles 16 de diciembre a las 12:00
    private final LocalDateTime earliest = LocalDateTime.of(2026, 12, 16, 12, 0);

    @BeforeEach
    void setUp() {
        service = new DeliveryOptionsService(cart, deliveryTime, scheduling);
    }

    private DeliverySlot slot(long id, int day, int hour, int capacity, int booked, boolean open) {
        return new DeliverySlot(id, LocalDate.of(2026, 12, day), LocalTime.of(hour, 0),
                LocalTime.of(hour + 2, 0), capacity, booked, open);
    }

    private void cartNeeds(int hours, int slotUnits) {
        when(cart.estimate(lines)).thenReturn(new CartWorkload(hours, slotUnits));
        when(deliveryTime.earliestDeliveryStart(hours, 7L)).thenReturn(earliest);
    }

    private List<Boolean> availability(DeliveryOptions options) {
        return options.slots().stream().map(SlotOption::available).toList();
    }

    @Test
    void returnsTheCartTimeAndTheEarliestStart() {
        cartNeeds(48, 1);
        when(scheduling.listSlots()).thenReturn(List.of());

        DeliveryOptions options = service.optionsFor(lines, 7L);

        assertThat(options.craftingHours()).isEqualTo(48);
        assertThat(options.earliestStart()).isEqualTo(earliest);
    }

    @Test
    void slotsThatStartTooSoonAreShownButNotAvailable() {
        cartNeeds(48, 1);
        when(scheduling.listSlots()).thenReturn(List.of(
                slot(1L, 15, 9, 4, 0, true),    // martes: muy pronto
                slot(2L, 16, 9, 4, 0, true),    // miércoles 9:00: antes de las 12:00
                slot(3L, 16, 14, 4, 0, true),   // miércoles 14:00: alcanza
                slot(4L, 17, 9, 4, 0, true)));  // jueves: alcanza

        assertThat(availability(service.optionsFor(lines, 7L)))
                .containsExactly(false, false, true, true);
    }

    @Test
    void slotStartingExactlyAtTheEarliestStartIsAvailable() {
        cartNeeds(48, 1);
        when(scheduling.listSlots()).thenReturn(List.of(slot(1L, 16, 12, 4, 0, true)));

        assertThat(availability(service.optionsFor(lines, 7L))).containsExactly(true);
    }

    @Test
    void slotWithoutEnoughRoomForTheCartIsNotAvailable() {
        // El carrito ocupa 3 cupos: en la primera quedan 2, en la segunda quedan 3
        cartNeeds(4, 3);
        when(scheduling.listSlots()).thenReturn(List.of(
                slot(1L, 17, 9, 4, 2, true),
                slot(2L, 17, 14, 4, 1, true)));

        assertThat(availability(service.optionsFor(lines, 7L))).containsExactly(false, true);
    }

    @Test
    void closedSlotIsNotAvailable() {
        cartNeeds(4, 1);
        when(scheduling.listSlots()).thenReturn(List.of(slot(1L, 17, 9, 4, 0, false)));

        assertThat(availability(service.optionsFor(lines, 7L))).containsExactly(false);
    }

    @Test
    void unavailableVariantStopsBeforeLookingAtSlots() {
        when(cart.estimate(lines)).thenThrow(new VariantNotAvailableException(1L));

        assertThatThrownBy(() -> service.optionsFor(lines, 7L))
                .isInstanceOf(VariantNotAvailableException.class);
        verifyNoInteractions(deliveryTime, scheduling);
    }

    @Test
    void unknownZoneStopsBeforeLookingAtSlots() {
        when(cart.estimate(lines)).thenReturn(new CartWorkload(4, 1));
        when(deliveryTime.earliestDeliveryStart(anyInt(), anyLong())).thenThrow(new ZoneNotFoundException(7L));

        assertThatThrownBy(() -> service.optionsFor(lines, 7L))
                .isInstanceOf(ZoneNotFoundException.class);
        verifyNoInteractions(scheduling);
    }
}