package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.domain.model.Order;

// Adaptador de salida: guarda pedidos con JPA. La transacción la abre el caso de uso (infra/config).
@Repository
class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final CustomerOrderJpaRepository jpa;
    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    OrderPersistenceAdapter(CustomerOrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(jpa.save(mapper.toEntity(order)));
    }

    // Trae también las líneas: si no hay una transacción abierta, se abre una solo de lectura.
    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByPublicCode(UUID publicCode) {
        return jpa.findByPublicCode(publicCode).map(mapper::toDomain);
    }

    // Se une a la transacción de quien lo llama (iniciar un pago); si no hay ninguna, abre una propia.
    @Override
    @Transactional
    public boolean updateHold(long orderId, Instant holdExpiresAt) {
        return jpa.updateHold(orderId, holdExpiresAt, OrderStatusColumn.PENDING_PAYMENT) == 1;
    }
}