package com.floralshop.backend.payment.infra.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface PaymentMethodPolicyJpaRepository extends JpaRepository<PaymentMethodPolicyJpaEntity, Short> {

    // A igual prioridad, el orden por pasarela y medio evita que la sugerida cambie al azar.
    List<PaymentMethodPolicyJpaEntity> findByEnabledTrueOrderByPriorityAscProviderAscMethodAsc();

    Optional<PaymentMethodPolicyJpaEntity> findByProviderAndMethodAndEnabledTrue(String provider,
                                                                                 PaymentMethodColumn method);
}
