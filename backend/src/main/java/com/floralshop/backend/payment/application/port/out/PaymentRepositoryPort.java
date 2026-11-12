package com.floralshop.backend.payment.application.port.out;

import java.util.Optional;

import com.floralshop.backend.payment.domain.model.Payment;

// Puerto de salida: guardar y buscar pagos.
public interface PaymentRepositoryPort {

    // Guarda el pago y devuelve la versión guardada (con id).
    Payment save(Payment payment);

    // El pago pendiente del pedido, si lo hay. Nunca hay más de uno (índice único en la tabla).
    Optional<Payment> findPendingByOrderId(long orderId);
}
