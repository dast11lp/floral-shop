package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
interface CustomerOrderJpaRepository extends JpaRepository<CustomerOrderJpaEntity, Long> {

    Optional<CustomerOrderJpaEntity> findByPublicCode(UUID publicCode);

    // Solo mueve el vencimiento de un pedido que sigue pendiente de pago, y nunca más allá de su tope:
    // la base lo comprueba aunque el servicio ya lo haya calculado. Devuelve cuántas filas cambió (0 o 1).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update CustomerOrderJpaEntity o set o.holdExpiresAt = :until
            where o.id = :id and o.status = :pending and :until <= o.holdLimitAt
            """)
    int updateHold(@Param("id") long id, @Param("until") Instant until, @Param("pending") OrderStatusColumn pending);
}