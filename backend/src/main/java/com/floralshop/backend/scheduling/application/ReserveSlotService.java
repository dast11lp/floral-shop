package com.floralshop.backend.scheduling.application;

import com.floralshop.backend.scheduling.domain.DeliverySlot;
import com.floralshop.backend.scheduling.domain.DeliverySlotRepository;
import com.floralshop.backend.scheduling.domain.SlotNotFoundException;

// Caso de uso: reservar cupo en una franja. Java puro, sin anotaciones de Spring.
// La transacción que hace que el bloqueo de fila funcione se añade en infra (sub-paso 2.4).
public class ReserveSlotService {

    private final DeliverySlotRepository slots;

    public ReserveSlotService(DeliverySlotRepository slots) {
        this.slots = slots;
    }

    public DeliverySlot reserve(long slotId, int units) {
        // 1. Cargar la franja, bloqueada. Si no existe, error de negocio.
        DeliverySlot slot = slots.findByIdForUpdate(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));

        // 2. Aplicar la regla (una sola vez, en el helper).
        SlotRules.checkCanReserve(slot, units);

        // 3. El record es inmutable: se crea una franja nueva con lo reservado actualizado.
        DeliverySlot updated = new DeliverySlot(slot.id(), slot.date(), slot.startTime(),
                slot.endTime(), slot.capacity(), slot.booked() + units, slot.open());

        // 4. Guardar y devolver la versión guardada.
        return slots.save(updated);
    }
}