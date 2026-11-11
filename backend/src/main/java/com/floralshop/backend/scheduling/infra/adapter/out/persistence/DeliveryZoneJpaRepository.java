package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface DeliveryZoneJpaRepository extends JpaRepository<DeliveryZoneJpaEntity, Long> {

    // Spring Data lo construye por el nombre: solo activas, ordenadas por nombre.
    List<DeliveryZoneJpaEntity> findByActiveTrueOrderByNameAsc();

    Optional<DeliveryZoneJpaEntity> findByIdAndActiveTrue(Long id);
}