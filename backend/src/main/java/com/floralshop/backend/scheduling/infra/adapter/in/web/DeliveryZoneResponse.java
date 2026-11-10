package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.math.BigDecimal;

import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Lo que ve el cliente de una zona. El id es necesario para elegirla al hacer el pedido (fase 3).
public record DeliveryZoneResponse(Long id, String name, BigDecimal shippingFee) {

    static DeliveryZoneResponse from(DeliveryZone zone) {
        return new DeliveryZoneResponse(zone.id(), zone.name(), zone.shippingFee());
    }
}