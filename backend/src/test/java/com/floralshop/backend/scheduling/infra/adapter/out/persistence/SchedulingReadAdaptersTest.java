package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.scheduling.application.port.out.DeliverySettingsRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DateRange;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Integración con Postgres real. @Transactional: cada prueba se deshace sola al terminar.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SchedulingReadAdaptersTest {

    @Autowired
    DeliverySlotRepositoryPort slots;

    @Autowired
    DeliveryZoneRepositoryPort zones;

    @Autowired
    DeliverySettingsRepositoryPort settings;

    @Autowired
    JdbcTemplate jdbc;

    private void slot(String date, String start, int capacity, int booked, boolean open) {
        jdbc.update("insert into delivery_slot (slot_date, start_time, end_time, capacity, booked, is_open) "
                + "values (?, ?, ?, ?, ?, ?)",
                LocalDate.parse(date), LocalTime.parse(start), LocalTime.parse(start).plusHours(2),
                capacity, booked, open);
    }

    private void zone(String name, String fee, boolean active) {
        jdbc.update("insert into delivery_zone (name, shipping_fee, active) values (?, ?, ?)",
                name, new BigDecimal(fee), active);
    }

    private DateRange december1To3() {
        return new DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
    }

    @Test
    void slotsInRangeAreOrderedAndIncludeBothEnds() {
        slot("2026-11-30", "09:00", 4, 0, true); // fuera: un día antes
        slot("2026-12-04", "09:00", 4, 0, true); // fuera: un día después
        slot("2026-12-03", "09:00", 4, 0, true); // dentro: último día
        slot("2026-12-01", "14:00", 4, 0, true);
        slot("2026-12-01", "09:00", 4, 0, true); // dentro: primer día

        assertThat(slots.findOpenByDateRange(december1To3()))
                .extracting(s -> s.date() + " " + s.startTime())
                .containsExactly("2026-12-01 09:00", "2026-12-01 14:00", "2026-12-03 09:00");
    }

    @Test
    void closedSlotsAreHidden() {
        slot("2026-12-01", "09:00", 4, 0, false);
        assertThat(slots.findOpenByDateRange(december1To3())).isEmpty();
    }

    @Test
    void fullSlotsAreIncludedWithNoAvailability() {
        slot("2026-12-01", "09:00", 2, 2, true);

        assertThat(slots.findOpenByDateRange(december1To3()))
                .extracting(DeliverySlot::available)
                .containsExactly(0);
    }

    @Test
    void listsOnlyActiveZonesOrderedByName() {
        zone("Norte", "10000", true);
        zone("Centro", "8000", true);
        zone("Antigua", "15000", false); // inactiva: no sale

        assertThat(zones.findActive())
                .extracting(DeliveryZone::name)
                .containsExactly("Centro", "Norte");
    }

    @Test
    void noZonesReturnsEmptyList() {
        assertThat(zones.findActive()).isEmpty();
    }

    private long zoneId(String name) {
        return jdbc.queryForObject("select id from delivery_zone where name = ?", Long.class, name);
    }

    @Test
    void findsActiveZoneByIdWithItsMargin() {
        zone("Chía", "15000", true);
        jdbc.update("update delivery_zone set margin_hours = 3 where name = 'Chía'");

        assertThat(zones.findActiveById(zoneId("Chía")))
                .get()
                .extracting(DeliveryZone::marginHours)
                .isEqualTo(3);
    }

    @Test
    void newZoneHasNoMargin() {
        zone("Centro", "8000", true);

        assertThat(zones.findActiveById(zoneId("Centro"))).get()
                .extracting(DeliveryZone::marginHours).isEqualTo(0);
    }

    @Test
    void inactiveZoneIsNotFoundById() {
        zone("Antigua", "15000", false);

        assertThat(zones.findActiveById(zoneId("Antigua"))).isEmpty();
    }

    @Test
    void generalMarginStartsAtTwoHours() {
        assertThat(settings.get().deliveryMarginHours()).isEqualTo(2);
    }

    @Test
    void generalMarginReflectsWhatTheFloristSaved() {
        jdbc.update("update shop_settings set delivery_margin_hours = 5");

        assertThat(settings.get().deliveryMarginHours()).isEqualTo(5);
    }
}