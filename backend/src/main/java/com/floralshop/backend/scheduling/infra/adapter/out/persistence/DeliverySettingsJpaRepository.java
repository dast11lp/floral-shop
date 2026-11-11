package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface DeliverySettingsJpaRepository extends JpaRepository<DeliverySettingsJpaEntity, Short> {
}