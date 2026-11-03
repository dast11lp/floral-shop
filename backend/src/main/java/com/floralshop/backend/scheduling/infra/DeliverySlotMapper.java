package com.floralshop.backend.scheduling.infra;

import com.floralshop.backend.scheduling.domain.DeliverySlot;


class DeliverySlotMapper {

    DeliverySlot toDomain(DeliverySlotEntity entity) {
        return new DeliverySlot(
                entity.getId(),
                entity.getSlotDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCapacity(),
                entity.getBooked(),
                entity.isOpen());
    }

    DeliverySlotEntity toEntity(DeliverySlot slot) {
        return new DeliverySlotEntity(
                slot.id(),
                slot.date(),
                slot.startTime(),
                slot.endTime(),
                slot.capacity(),
                slot.booked(),
                slot.open());
    }
}