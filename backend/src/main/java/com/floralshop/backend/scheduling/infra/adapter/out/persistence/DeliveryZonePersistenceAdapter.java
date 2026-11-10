package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Adaptador de salida: implementa el puerto de zonas con JPA.
// La conversión es de una sola línea, así que va dentro del adapter, sin una clase mapper aparte.
@Repository
class DeliveryZonePersistenceAdapter implements DeliveryZoneRepositoryPort {

    private final DeliveryZoneJpaRepository jpa;

    DeliveryZonePersistenceAdapter(DeliveryZoneJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<DeliveryZone> findActive() {
        return jpa.findByActiveTrueOrderByNameAsc().stream()
                .map(zone -> new DeliveryZone(zone.getId(), zone.getName(), zone.getShippingFee(), zone.isActive()))
                .toList();
    }
}