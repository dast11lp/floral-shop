package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface CustomerOrderJpaRepository extends JpaRepository<CustomerOrderJpaEntity, Long> {
}