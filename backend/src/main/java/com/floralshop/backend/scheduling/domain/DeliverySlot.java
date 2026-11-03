package com.floralshop.backend.scheduling.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

// Franja de entrega con cupo. Modelo de datos inmutable: sin Spring, sin JPA.
// Solo valida su propia consistencia. Las decisiones del negocio
// (por ejemplo, si cabe una reserva) viven en la capa application.
public record DeliverySlot(Long id, LocalDate date, LocalTime startTime, LocalTime endTime,
                           int capacity, int booked, boolean open) {

    // Constructor compacto: se ejecuta cada vez que se crea una franja.
    public DeliverySlot {
        Objects.requireNonNull(date, "La fecha es obligatoria");
        Objects.requireNonNull(startTime, "La hora de inicio es obligatoria");
        Objects.requireNonNull(endTime, "La hora de fin es obligatoria");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("La franja debe terminar después de empezar");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        if (booked < 0 || booked > capacity) {
            throw new IllegalArgumentException("Lo reservado debe estar entre 0 y la capacidad");
        }
    }

    // Cupos que todavía quedan. Es un dato derivado, no una decisión de negocio.
    public int available() {
        return capacity - booked;
    }
}