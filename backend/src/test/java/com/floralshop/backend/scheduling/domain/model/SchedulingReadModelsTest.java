package com.floralshop.backend.scheduling.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.floralshop.backend.scheduling.domain.exception.InvalidDateRangeException;

// Java puro: comprueba las reglas de consistencia de DateRange y DeliveryZone.
class SchedulingReadModelsTest {

    private final LocalDate start = LocalDate.of(2026, 12, 1);

    // ---- DateRange ----

    @Test
    void singleDayRangeIsValid() {
        assertThat(new DateRange(start, start).to()).isEqualTo(start);
    }

    @Test
    void rangeOfExactlyMaxDaysIsValid() {
        // del 1 al 31 de diciembre: 31 días con los dos extremos incluidos
        assertThat(new DateRange(start, start.plusDays(DateRange.MAX_DAYS - 1)).to())
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void rangeOverMaxDaysIsRejected() {
        assertThatThrownBy(() -> new DateRange(start, start.plusDays(DateRange.MAX_DAYS)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> new DateRange(start, start.minusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    // ---- DeliveryZone ----

    @Test
    void zoneAcceptsZeroShippingFee() {
        assertThat(new DeliveryZone(1L, "Centro", BigDecimal.ZERO, true).shippingFee()).isZero();
    }

    @Test
    void zoneRejectsNegativeShippingFee() {
        assertThatThrownBy(() -> new DeliveryZone(1L, "Centro", new BigDecimal("-1"), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zoneRejectsBlankName() {
        assertThatThrownBy(() -> new DeliveryZone(1L, " ", BigDecimal.TEN, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}