package com.floralshop.backend.payment.infra.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface PaymentSettingsJpaRepository extends JpaRepository<PaymentSettingsJpaEntity, Short> {
}
