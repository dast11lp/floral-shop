package com.floralshop.backend.payment.infra.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repositorio de Spring Data. Detalle de infraestructura, sin visibilidad pública.
// Todos los valores van como parámetros (:nombre), nunca pegados en el texto: así no hay inyección SQL.
interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {

    Optional<PaymentJpaEntity> findByOrderIdAndStatus(Long orderId, PaymentStatusColumn status);

    // Usa el índice ix_payment_order (V10).
    @Query("select count(p) from PaymentJpaEntity p where p.orderId = :orderId and p.providerReference is not null")
    long countWithReference(@Param("orderId") long orderId);

    // Actualizaciones condicionales de una sola fila (ADR 0016, D.8): la base revisa el estado y cambia
    // la fila en la misma sentencia, así que nada se cuela entre la revisión y el cambio.
    // Devuelven cuántas filas cambiaron: 1, o 0 si el pago ya no estaba como se esperaba.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentJpaEntity p
            set p.providerReference = :reference, p.checkoutUrl = :url, p.checkoutExpiresAt = :expiresAt
            where p.id = :id and p.status = :expected
            """)
    int attachCheckoutIf(@Param("id") long id, @Param("expected") PaymentStatusColumn expected,
                         @Param("reference") String reference, @Param("url") String url,
                         @Param("expiresAt") Instant expiresAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentJpaEntity p set p.status = :next, p.statusReason = :reason, p.resolvedAt = :resolvedAt
            where p.id = :id and p.status = :expected
            """)
    int closeIf(@Param("id") long id, @Param("expected") PaymentStatusColumn expected,
                @Param("next") PaymentStatusColumn next, @Param("reason") PaymentStatusReasonColumn reason,
                @Param("resolvedAt") Instant resolvedAt);

    // Como closeIf, pero además exige que no tenga referencia y que se haya creado antes de createdBefore (D.3).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentJpaEntity p set p.status = :next, p.statusReason = :reason, p.resolvedAt = :resolvedAt
            where p.id = :id and p.status = :expected and p.providerReference is null
              and p.createdAt < :createdBefore
            """)
    int closeWithoutReferenceIfOlderThan(@Param("id") long id, @Param("expected") PaymentStatusColumn expected,
                                         @Param("createdBefore") Instant createdBefore,
                                         @Param("next") PaymentStatusColumn next,
                                         @Param("reason") PaymentStatusReasonColumn reason,
                                         @Param("resolvedAt") Instant resolvedAt);
}
