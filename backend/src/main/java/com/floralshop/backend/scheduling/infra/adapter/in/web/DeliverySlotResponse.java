package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.time.LocalDate;
import java.time.LocalTime;

import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.SlotOption;

// Lo que ve el cliente de una franja. No expone capacity ni booked, solo si tiene cupo.
public record DeliverySlotResponse(Long id, LocalDate date, LocalTime startTime, LocalTime endTime,
                                   boolean available) {

    static DeliverySlotResponse from(DeliverySlot slot) {
        return new DeliverySlotResponse(slot.id(), slot.date(), slot.startTime(), slot.endTime(),
                slot.open() && slot.available() > 0);
    }

    // Para un carrito: available ya viene decidido por DeliveryOptionsService.
    static DeliverySlotResponse from(SlotOption option) {
        DeliverySlot slot = option.slot();
        return new DeliverySlotResponse(slot.id(), slot.date(), slot.startTime(), slot.endTime(),
                option.available());
    }
}