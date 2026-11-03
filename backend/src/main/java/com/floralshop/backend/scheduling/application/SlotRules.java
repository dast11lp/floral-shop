package com.floralshop.backend.scheduling.application;

import com.floralshop.backend.scheduling.domain.DeliverySlot;
import com.floralshop.backend.scheduling.domain.SlotNotAvailableException;

// Helper: aquí está escrita UNA sola vez la regla de "se puede reservar en esta franja".
// Todo servicio que reserve cupo debe llamar a checkCanReserve en vez de repetir los if.
final class SlotRules {

    private SlotRules() {
        // clase de utilidad: no se instancia
    }

    static void checkCanReserve(DeliverySlot slot, int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("Las unidades a reservar deben ser mayores que cero");
        }
        if (!slot.open()) {
            throw new SlotNotAvailableException("La franja está cerrada");
        }
        if (units > slot.available()) {
            throw new SlotNotAvailableException(
                    "No hay cupo suficiente: quedan " + slot.available() + " y se piden " + units);
        }
    }
}