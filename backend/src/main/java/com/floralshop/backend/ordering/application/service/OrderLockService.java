package com.floralshop.backend.ordering.application.service;

import java.util.UUID;

import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.domain.exception.OrderNotFoundException;
import com.floralshop.backend.ordering.domain.model.Order;

// El candado del pedido para otros módulos (por ejemplo, pagos): SELECT ... FOR UPDATE (ADR 0016, D.7).
// Mientras dure la transacción de quien lo pide, nadie más puede cambiar ese pedido; quien llegue después
// espera su turno y lee lo que dejó el primero. Java puro, sin anotaciones de Spring.
public class OrderLockService {

    private final OrderRepositoryPort orders;

    public OrderLockService(OrderRepositoryPort orders) {
        this.orders = orders;
    }

    // Tiene que ser la PRIMERA lectura del pedido dentro de la transacción: si antes se leyó sin candado,
    // Hibernate devuelve la copia que ya tenía, con datos viejos (ADR 0016, "Cuidado con Hibernate").
    // Sin una transacción abierta falla: un candado que se suelta al instante no protege nada.
    public Order lockByPublicCode(UUID publicCode) {
        return orders.findByPublicCodeForUpdate(publicCode).orElseThrow(() -> new OrderNotFoundException(publicCode));
    }
}
