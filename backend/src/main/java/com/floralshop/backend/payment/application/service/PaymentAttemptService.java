package com.floralshop.backend.payment.application.service;

import java.time.Clock;
import java.time.Duration;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;

// Los pasos de un intento de pago que tocan la base de datos. Cada método es una transacción corta,
// declarada en infra/config (ADR 0011). La llamada a la pasarela va entre ellos, fuera de toda transacción
// (ADR 0014: los efectos hacia afuera no van dentro de la transacción). Java puro, sin anotaciones de Spring.
public class PaymentAttemptService {

    private final PaymentRepositoryPort payments;
    private final OrderHoldService holds;
    private final Clock clock;

    public PaymentAttemptService(PaymentRepositoryPort payments, OrderHoldService holds, Clock clock) {
        this.payments = payments;
        this.holds = holds;
        this.clock = clock;
    }

    // Paso 1: guarda el intento, todavía sin cobro, y extiende la franja del pedido los minutos del medio.
    // Si otra petición ya tiene un pago pendiente para este pedido, el índice único lo impide aquí,
    // antes de llamar a la pasarela (PaymentAlreadyInProgressException).
    public Payment reserve(Order order, PaymentMethodPolicy policy) {
        Payment attempt = payments.save(new Payment(null, order.id(), policy.provider(), policy.method(),
                PaymentStatus.PENDING, order.total(), null, null));
        holds.extendHold(order, clock.instant().plus(Duration.ofMinutes(policy.holdMinutes())));
        return attempt;
    }

    // Paso 3, si la pasarela creó el cobro: se anotan su id allá y la URL de pago.
    public Payment attachCheckout(Payment attempt, CheckoutSession session) {
        return payments.save(attempt.withCheckout(session.providerReference(), session.checkoutUrl()));
    }

    // Paso 3, si la pasarela falló: el intento queda fallido, no a medias, y el pedido puede intentarlo de nuevo.
    public void markFailed(Payment attempt) {
        payments.save(attempt.transitionTo(PaymentStatus.FAILED));
    }
}
