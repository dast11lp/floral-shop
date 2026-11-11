package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import org.springframework.stereotype.Repository;

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
}