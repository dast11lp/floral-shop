package com.floralshop.backend.scheduling.infra;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.scheduling.domain.DeliverySlot;
import com.floralshop.backend.scheduling.domain.DeliverySlotRepository;

// Adaptador: implementa el puerto del dominio con JPA.
// El negocio solo conoce DeliverySlotRepository; esta clase es un detalle de infraestructura.
@Repository
class JpaDeliverySlotRepository implements DeliverySlotRepository {

    private final DeliverySlotJpaRepository jpa;
    private final DeliverySlotMapper mapper = new DeliverySlotMapper();

    JpaDeliverySlotRepository(DeliverySlotJpaRepository jpa) {
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
        DeliverySlotEntity saved = jpa.save(mapper.toEntity(slot));
        return mapper.toDomain(saved);
    }
}