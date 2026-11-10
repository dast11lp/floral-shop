package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.time.LocalDate;
import java.time.LocalTime;

import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Lo que ve el cliente de una franja. No expone capacity ni booked, solo si tiene cupo.
public record DeliverySlotResponse(Long id, LocalDate date, LocalTime startTime, LocalTime endTime,
                                   boolean available) {

    static DeliverySlotResponse from(DeliverySlot slot) {
        return new DeliverySlotResponse(slot.id(), slot.date(), slot.startTime(), slot.endTime(),
                slot.open() && slot.available() > 0);
    }
}