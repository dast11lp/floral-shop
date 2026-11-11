package com.floralshop.backend.scheduling.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DateRange;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

@ExtendWith(MockitoExtension.class)
class SchedulingQueryServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    // 10 de diciembre de 2026, 10:00 en Bogotá (15:00 UTC)
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T15:00:00Z"), BOGOTA);
    private static final LocalDate TODAY = LocalDate.of(2026, 12, 10);
    private static final DateRange FULL_RANGE = new DateRange(TODAY, TODAY.plusDays(DateRange.MAX_DAYS - 1));

    @Mock
    DeliverySlotRepositoryPort slots;

    @Mock
    DeliveryZoneRepositoryPort zones;

    SchedulingQueryService service;

    @BeforeEach
    void setUp() {
        service = new SchedulingQueryService(slots, zones, CLOCK);
    }

    private List<DeliverySlot> someSlots() {
        return List.of(new DeliverySlot(1L, TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 4, 0, true));
    }

    @Test
    void listsSlotsFromTodayForTheMaximumRange() {
        List<DeliverySlot> expected = someSlots();
        when(slots.findOpenByDateRange(FULL_RANGE)).thenReturn(expected);

        assertThat(service.listSlots()).isSameAs(expected);
    }

    @Test
    void todayIsComputedInTheClockZoneNotUtc() {
        // 21:00 del 10 de diciembre en Bogotá = 02:00 UTC del 11. "Hoy" debe seguir siendo el 10.
        Clock lateNight = Clock.fixed(Instant.parse("2026-12-11T02:00:00Z"), BOGOTA);
        when(slots.findOpenByDateRange(FULL_RANGE)).thenReturn(someSlots());

        new SchedulingQueryService(slots, zones, lateNight).listSlots();

        verify(slots).findOpenByDateRange(FULL_RANGE);
    }

    @Test
    void listsZonesFromThePort() {
        List<DeliveryZone> expected = List.of(new DeliveryZone(1L, "Centro", new BigDecimal("8000"), true, 0));
        when(zones.findActive()).thenReturn(expected);

        assertThat(service.listZones()).isSameAs(expected);
    }
}