package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

// Repositorio de Spring Data: detalle de infraestructura, sin visibilidad pública.
// El resto del sistema usa el puerto DeliverySlotRepository, que lo implementa un adaptador (paso 5).
interface DeliverySlotJpaRepository extends JpaRepository<DeliverySlotJpaEntity, Long> {

    // PESSIMISTIC_WRITE se traduce en SELECT ... FOR UPDATE: la fila queda bloqueada
    // hasta que termine la transacción, y otra petición que pida la misma fila espera.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DeliverySlotJpaEntity s where s.id = :id")
    Optional<DeliverySlotJpaEntity> findByIdForUpdate(@Param("id") long id);

        // Franjas abiertas en el rango (BETWEEN incluye los dos extremos), por fecha y hora de inicio.
    @Query("""
            select s from DeliverySlotJpaEntity s
            where s.slotDate between :from and :to and s.open = true
            order by s.slotDate, s.startTime
            """)
    List<DeliverySlotJpaEntity> findOpenBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}