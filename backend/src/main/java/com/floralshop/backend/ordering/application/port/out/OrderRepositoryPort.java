package com.floralshop.backend.ordering.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.floralshop.backend.ordering.domain.model.Order;

// Puerto de salida: guardar y buscar pedidos.
public interface OrderRepositoryPort {

    // Guarda el pedido con sus líneas y devuelve la versión guardada (con id).
    Order save(Order order);

    Optional<Order> findByPublicCode(UUID publicCode);

    // Mueve el vencimiento de la franja de un pedido pendiente de pago, sin pasar de su tope.
    // Devuelve false si el pedido ya no está pendiente o si la fecha pasa del tope.
    boolean updateHold(long orderId, Instant holdExpiresAt);
}