package com.floralshop.backend.scheduling.infra.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;

import jakarta.validation.Valid;

// Adaptador de entrada: las franjas para un carrito. No decide nada.
// Es POST porque el carrito va en el cuerpo, pero no guarda ni aparta nada:
// apartar el cupo es parte de crear el pedido.
@RestController
@RequestMapping("/api/v1")
public class DeliveryOptionsController {

    private final DeliveryOptionsService options;

    public DeliveryOptionsController(DeliveryOptionsService options) {
        this.options = options;
    }

    @PostMapping("/delivery-options")
    public DeliveryOptionsResponse deliveryOptions(@Valid @RequestBody DeliveryOptionsRequest request) {
        return DeliveryOptionsResponse.from(options.optionsFor(request.toCartLines(), request.zoneId()));
    }
}