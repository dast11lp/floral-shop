package com.floralshop.backend.payment.application.port.out;

import java.math.BigDecimal;

import com.floralshop.backend.payment.domain.model.PaymentMethod;

// Lo que una pasarela necesita para crear un cobro. Sin datos personales del cliente:
// la pasarela se los pide en su propia página.
// reference: nuestra referencia del cobro (el código público del pedido).
// idempotencyKey: una por intento de pago. Si la misma petición se repite (por ejemplo, tras un fallo de red),
// la pasarela devuelve el mismo cobro en vez de crear otro.
public record CheckoutRequest(String reference, String description, BigDecimal amount, PaymentMethod method,
                              String idempotencyKey) {
}
