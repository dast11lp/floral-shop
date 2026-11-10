package com.floralshop.backend.scheduling.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.floralshop.backend.scheduling.domain.exception.InvalidDateRangeException;

public record DateRange(LocalDate from, LocalDate to) {

    // Tope de días, para que nadie pida meses de franjas de un golpe.
    public static final int MAX_DAYS = 31;

    public DateRange {
        Objects.requireNonNull(from, "La fecha inicial es obligatoria");
        Objects.requireNonNull(to, "La fecha final es obligatoria");
        if (to.isBefore(from)) {
            throw new InvalidDateRangeException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidDateRangeException("El rango no puede superar " + MAX_DAYS + " días");
        }
    }
}