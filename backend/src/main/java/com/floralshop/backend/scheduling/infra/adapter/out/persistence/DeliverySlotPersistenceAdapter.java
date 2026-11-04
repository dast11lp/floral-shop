package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Adaptador: implementa el puerto del dominio con JPA.
// El negocio solo conoce DeliverySlotRepository; esta clase es un detalle de infraestructura.
@Repository
class DeliverySlotPersistenceAdapter implements DeliverySlotRepositoryPort {

    private final DeliverySlotJpaRepository jpa;
    private final DeliverySlotPersistenceMapper mapper = new DeliverySlotPersistenceMapper();

    DeliverySlotPersistenceAdapter(DeliverySlotJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<DeliverySlot> findByIdForUpdate(long id) {
        // Pide la fila bloqueada y la convierte a dominio si existe.
        return jpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public DeliverySlot save(DeliverySlot slot) {
        // Dominio -> entidad -> guardar -> la versión guardada, de vuelta a dominio.
        DeliverySlotJpaEntity saved = jpa.save(mapper.toEntity(slot));
        return mapper.toDomain(saved);
    }
}