package com.floralshop.backend.scheduling.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DateRange;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Consultas de franjas y zonas. Java puro, sin anotaciones de Spring.
public class SchedulingQueryService {

    private final DeliverySlotRepositoryPort slots;
    private final DeliveryZoneRepositoryPort zones;
    private final Clock clock; // trae su zona horaria: en producción, la de Bogotá

    public SchedulingQueryService(DeliverySlotRepositoryPort slots, DeliveryZoneRepositoryPort zones, Clock clock) {
        this.slots = slots;
        this.zones = zones;
        this.clock = clock;
    }

    // Franjas abiertas desde hoy hasta el máximo del rango (31 días), "hoy" según la zona del reloj.
    public List<DeliverySlot> listSlots() {
        LocalDate today = LocalDate.now(clock);
        return slots.findOpenByDateRange(new DateRange(today, today.plusDays(DateRange.MAX_DAYS - 1)));
    }

    public List<DeliveryZone> listZones() {
        return zones.findActive();
    }
}