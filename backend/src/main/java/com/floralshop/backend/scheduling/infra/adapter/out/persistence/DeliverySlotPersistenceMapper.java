package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import com.floralshop.backend.scheduling.domain.model.DeliverySlot;


class DeliverySlotPersistenceMapper {

    DeliverySlot toDomain(DeliverySlotJpaEntity entity) {
        return new DeliverySlot(
                entity.getId(),
                entity.getSlotDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCapacity(),
                entity.getBooked(),
                entity.isOpen());
    }

    DeliverySlotJpaEntity toEntity(DeliverySlot slot) {
        return new DeliverySlotJpaEntity(
                slot.id(),
                slot.date(),
                slot.startTime(),
                slot.endTime(),
                slot.capacity(),
                slot.booked(),
                slot.open());
    }
}