package com.floralshop.backend.payment.application.service;

import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.payment.domain.model.Payment;

// Lo que deja el paso 1 de un intento de pago (PaymentAttemptService.reserve):
// - order: el pedido tal como se leyó con el candado;
// - payment: el intento nuevo, o el pago pendiente que ya tenía su enlace con la misma opción;
// - reused: true en el segundo caso. Entonces no se llama a la pasarela: el cliente recibe el mismo enlace.
public record PaymentReservation(Order order, Payment payment, boolean reused) {
}
