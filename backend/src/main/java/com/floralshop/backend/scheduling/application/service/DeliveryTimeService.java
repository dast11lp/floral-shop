package com.floralshop.backend.scheduling.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.floralshop.backend.scheduling.application.port.out.DeliverySettingsRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.exception.ZoneNotFoundException;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Desde cuándo se puede entregar un pedido (ADR 0012, punto 5). Java puro, sin anotaciones de Spring.
//
//   inicio más temprano = ahora + elaboración del carrito + margen de despacho + margen de viaje de la zona
//
// Todos los días cuentan, también los que la florista no trabaja (los maneja cerrando franjas).
// Si eso cambia (pendiente 23), se cambia este servicio.
public class DeliveryTimeService {

    private final DeliveryZoneRepositoryPort zones;
    private final DeliverySettingsRepositoryPort settings;
    private final Clock clock; // trae su zona horaria: en producción, la de Bogotá

    public DeliveryTimeService(DeliveryZoneRepositoryPort zones, DeliverySettingsRepositoryPort settings,
                               Clock clock) {
        this.zones = zones;
        this.settings = settings;
        this.clock = clock;
    }

    // craftingHours es el tiempo del carrito completo (CartWorkloadService, en catalog).
    public LocalDateTime earliestDeliveryStart(int craftingHours, long zoneId) {
        if (craftingHours < 0) {
            throw new IllegalArgumentException("El tiempo de elaboración no puede ser negativo");
        }
        DeliveryZone zone = zones.findActiveById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        long totalHours = (long) craftingHours + settings.get().dispatchMarginHours() + zone.travelMarginHours();
        return LocalDateTime.now(clock).plusHours(totalHours);
    }

    // Una franja alcanza si empieza en el inicio más temprano o después.
    public static boolean startsInTime(DeliverySlot slot, LocalDateTime earliestStart) {
        return !LocalDateTime.of(slot.date(), slot.startTime()).isBefore(earliestStart);
    }
}