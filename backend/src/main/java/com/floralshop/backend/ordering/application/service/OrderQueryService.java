package com.floralshop.backend.ordering.application.service;

import java.util.UUID;

import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.domain.exception.OrderNotFoundException;
import com.floralshop.backend.ordering.domain.model.Order;

// Consultas de pedidos para otros módulos (por ejemplo, pagos). Java puro, sin anotaciones de Spring.
public class OrderQueryService {

    private final OrderRepositoryPort orders;

    public OrderQueryService(OrderRepositoryPort orders) {
        this.orders = orders;
    }

    // Un pedido por su código público. Si no existe, error de negocio.
    public Order getByPublicCode(UUID publicCode) {
        return orders.findByPublicCode(publicCode).orElseThrow(() -> new OrderNotFoundException(publicCode));
    }
}
