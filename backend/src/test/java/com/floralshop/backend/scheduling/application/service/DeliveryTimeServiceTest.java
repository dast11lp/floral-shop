package com.floralshop.backend.scheduling.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.scheduling.application.port.out.DeliverySettingsRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;
import com.floralshop.backend.scheduling.domain.model.DeliverySettings;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// inicio más temprano = ahora + preparación + margen general + margen de la zona (ADR 0012, punto 5).
@ExtendWith(MockitoExtension.class)
class DeliveryTimeServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    // Lunes 14 de diciembre de 2026, 10:00 en Bogotá (15:00 UTC)
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-14T15:00:00Z"), BOGOTA);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 14, 10, 0);

    @Mock
    DeliveryZoneRepositoryPort zones;

    @Mock
    DeliverySettingsRepositoryPort settings;

    DeliveryTimeService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryTimeService(zones, settings, CLOCK);
    }

    private void zoneWithMargin(long id, int marginHours) {
        when(zones.findActiveById(id))
                .thenReturn(Optional.of(new DeliveryZone(id, "Zona", new BigDecimal("8000"), true, marginHours)));
    }

    private void generalMargin(int hours) {
        when(settings.get()).thenReturn(new DeliverySettings(hours));
    }

    @Test
    void addsPreparationAndBothMargins() {
        // 48 h + 2 h generales + 2 h de la zona = 52 h: miércoles 16, 14:00
        generalMargin(2);
        zoneWithMargin(1L, 2);

        assertThat(service.earliestDeliveryStart(48, 1L)).isEqualTo(LocalDateTime.of(2026, 12, 16, 14, 0));
    }

    @Test
    void zoneWithoutMarginAddsNothing() {
        generalMargin(2);
        zoneWithMargin(1L, 0);

        assertThat(service.earliestDeliveryStart(4, 1L)).isEqualTo(NOW.plusHours(6));
    }

    @Test
    void readyNowStillWaitsForTheGeneralMargin() {
        generalMargin(2);
        zoneWithMargin(1L, 0);

        assertThat(service.earliestDeliveryStart(0, 1L)).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void unknownOrInactiveZoneIsRejected() {
        when(zones.findActiveById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.earliestDeliveryStart(4, 9L))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void negativePreparationIsRejected() {
        assertThatThrownBy(() -> service.earliestDeliveryStart(-1, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- startsInTime: la franja debe empezar en el inicio más temprano o después ----

    private DeliverySlot slotAt(int day, int hour) {
        return new DeliverySlot(1L, LocalDate.of(2026, 12, day), LocalTime.of(hour, 0),
                LocalTime.of(hour + 2, 0), 4, 0, true);
    }

    @Test
    void slotStartingExactlyAtTheEarliestStartIsInTime() {
        assertThat(DeliveryTimeService.startsInTime(slotAt(16, 14), LocalDateTime.of(2026, 12, 16, 14, 0)))
                .isTrue();
    }

    @Test
    void slotStartingBeforeTheEarliestStartIsNotInTime() {
        // El ejemplo del ADR: el carrito pide 48 h y la franja es mañana
        assertThat(DeliveryTimeService.startsInTime(slotAt(15, 9), LocalDateTime.of(2026, 12, 16, 12, 0)))
                .isFalse();
    }

    @Test
    void slotStartingAfterTheEarliestStartIsInTime() {
        assertThat(DeliveryTimeService.startsInTime(slotAt(17, 9), LocalDateTime.of(2026, 12, 16, 12, 0)))
                .isTrue();
    }
}