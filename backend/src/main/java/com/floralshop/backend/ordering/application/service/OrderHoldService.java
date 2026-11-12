package com.floralshop.backend.ordering.application.service;

import java.time.Instant;

import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderStatus;

// Cuánto tiempo retiene su franja un pedido pendiente de pago (ADR 0012: vencer el cupo es asunto del pedido).
// Lo usa pagos al iniciar un intento de pago. Java puro, sin anotaciones de Spring.
public class OrderHoldService {

    private final OrderRepositoryPort orders;

    public OrderHoldService(OrderRepositoryPort orders) {
        this.orders = orders;
    }

    // Extiende el vencimiento hasta "requested", con dos reglas:
    // - nunca más allá del tope fijado al crear el pedido (así nadie retiene una franja para siempre);
    // - nunca lo acorta: si ya vence después, se deja como está.
    // Devuelve el vencimiento que quedó. Un pedido que ya no está pendiente de pago no se toca.
    public Instant extendHold(Order order, Instant requested) {
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            return order.holdExpiresAt();
        }
        Instant target = requested.isAfter(order.holdLimitAt()) ? order.holdLimitAt() : requested;
        if (!target.isAfter(order.holdExpiresAt())) {
            return order.holdExpiresAt();
        }
        return orders.updateHold(order.id(), target) ? target : order.holdExpiresAt();
    }
}
