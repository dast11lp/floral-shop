package com.floralshop.backend.scheduling.infra.config;

import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Misma lógica que ReserveSlotService, pero ejecutada dentro de una transacción.
// El bloqueo de fila (FOR UPDATE) dura lo que dure esta transacción.
// No añade lógica: solo declara la transacción, que application no puede declarar (ADR 0011).
class TransactionalReserveSlotService extends ReserveSlotService {

    TransactionalReserveSlotService(DeliverySlotRepositoryPort slots) {
        super(slots);
    }

    @Override
    @Transactional
    public DeliverySlot reserve(long slotId, int units) {
        return super.reserve(slotId, units);
    }
}