package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;

import com.floralshop.backend.scheduling.domain.model.DeliveryOptions;

// Lo que ve el cliente: cuánto tarda su carrito (en horas; el front decide cómo mostrarlo),
// desde cuándo se puede entregar y las franjas, con available = false en las que no alcanzan.
// Como en GET /delivery-slots, no expone capacidad, reservas ni cupos que ocupa.
public record DeliveryOptionsResponse(int craftingHours, LocalDateTime earliestStart,
                                      List<DeliverySlotResponse> slots) {

    static DeliveryOptionsResponse from(DeliveryOptions options) {
        return new DeliveryOptionsResponse(options.craftingHours(), options.earliestStart(),
                options.slots().stream().map(DeliverySlotResponse::from).toList());
    }
}