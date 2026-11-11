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
//   inicio más temprano = ahora + preparación del carrito + margen general + margen de la zona
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

    // preparationHours es el tiempo del carrito completo (CartPreparationService, en catalog).
    public LocalDateTime earliestDeliveryStart(int preparationHours, long zoneId) {
        if (preparationHours < 0) {
            throw new IllegalArgumentException("El tiempo de preparación no puede ser negativo");
        }
        DeliveryZone zone = zones.findActiveById(zoneId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));
        long totalHours = (long) preparationHours + settings.get().deliveryMarginHours() + zone.marginHours();
        return LocalDateTime.now(clock).plusHours(totalHours);
    }

    // Una franja alcanza si empieza en el inicio más temprano o después.
    public static boolean startsInTime(DeliverySlot slot, LocalDateTime earliestStart) {
        return !LocalDateTime.of(slot.date(), slot.startTime()).isBefore(earliestStart);
    }
}