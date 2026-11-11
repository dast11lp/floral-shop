package com.floralshop.backend.ordering.application.port.out;

import com.floralshop.backend.ordering.domain.model.Order;

// Puerto de salida: guardar pedidos.
public interface OrderRepositoryPort {

    // Guarda el pedido con sus líneas y devuelve la versión guardada (con id).
    Order save(Order order);
}