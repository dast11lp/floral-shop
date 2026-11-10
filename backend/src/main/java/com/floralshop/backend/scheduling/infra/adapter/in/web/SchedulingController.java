package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;

// Adaptador de entrada: traduce HTTP a llamadas al servicio. No decide nada.
@RestController
@RequestMapping("/api/v1")
public class SchedulingController {

    private final SchedulingQueryService scheduling;

    public SchedulingController(SchedulingQueryService scheduling) {
        this.scheduling = scheduling;
    }

    // Franjas abiertas desde hoy hasta dentro de 31 días. El frontend las pinta como calendario.
    @GetMapping("/delivery-slots")
    public List<DeliverySlotResponse> listSlots() {
        return scheduling.listSlots().stream().map(DeliverySlotResponse::from).toList();
    }

    @GetMapping("/delivery-zones")
    public List<DeliveryZoneResponse> listZones() {
        return scheduling.listZones().stream().map(DeliveryZoneResponse::from).toList();
    }
}