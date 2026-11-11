package com.floralshop.backend.scheduling.application.service;

import java.time.LocalDateTime;
import java.util.List;

import com.floralshop.backend.catalog.application.service.CartWorkloadService;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.CartWorkload;
import com.floralshop.backend.scheduling.domain.model.DeliveryOptions;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.SlotOption;

// Las franjas para un carrito y una zona (ADR 0012, puntos 1 y 5). Java puro, sin anotaciones de Spring.
//
// Junta tres piezas que ya existen, sin repetir sus reglas:
//   1. cuánto trabajo pide el carrito       -> CartWorkloadService (catalog)
//   2. desde cuándo se puede entregar        -> DeliveryTimeService
//   3. qué franjas hay en el horizonte       -> SchedulingQueryService
// Una franja alcanza si empieza a tiempo, está abierta y le caben los cupos del carrito.
// Las que no alcanzan se devuelven igual, marcadas, para que el cliente las vea deshabilitadas.
public class DeliveryOptionsService {

    private final CartWorkloadService cart;
    private final DeliveryTimeService deliveryTime;
    private final SchedulingQueryService scheduling;

    public DeliveryOptionsService(CartWorkloadService cart, DeliveryTimeService deliveryTime,
                                  SchedulingQueryService scheduling) {
        this.cart = cart;
        this.deliveryTime = deliveryTime;
        this.scheduling = scheduling;
    }

    public DeliveryOptions optionsFor(List<CartLine> lines, long zoneId) {
        CartWorkload work = cart.estimate(lines);
        LocalDateTime earliest = deliveryTime.earliestDeliveryStart(work.craftingHours(), zoneId);
        List<SlotOption> slots = scheduling.listSlots().stream()
                .map(slot -> new SlotOption(slot, fits(slot, earliest, work.slotUnits())))
                .toList();
        return new DeliveryOptions(work.craftingHours(), earliest, slots);
    }

    private static boolean fits(DeliverySlot slot, LocalDateTime earliest, int slotUnits) {
        return DeliveryTimeService.startsInTime(slot, earliest)
                && slot.open()
                && slot.available() >= slotUnits;
    }
}