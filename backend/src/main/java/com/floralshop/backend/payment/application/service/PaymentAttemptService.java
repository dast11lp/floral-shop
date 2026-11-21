package com.floralshop.backend.payment.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.application.service.OrderLockService;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentSettingsRepositoryPort;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.exception.PaymentAttemptClosedException;
import com.floralshop.backend.payment.domain.exception.PaymentAttemptsExhaustedException;
import com.floralshop.backend.payment.domain.exception.PaymentStartingException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Los pasos de un intento de pago que tocan la base de datos. Cada método es una transacción corta,
// declarada en infra/config (ADR 0011). La llamada a la pasarela va entre ellos, fuera de toda transacción
// (ADR 0014: los efectos hacia afuera no van dentro de la transacción). Java puro, sin anotaciones de Spring.
public class PaymentAttemptService {

    private final PaymentRepositoryPort payments;
    private final PaymentSettingsRepositoryPort settings;
    private final OrderLockService orderLocks;
    private final OrderHoldService holds;
    private final Clock clock;
    private final Duration creationGrace;

    public PaymentAttemptService(PaymentRepositoryPort payments, PaymentSettingsRepositoryPort settings,
                                 OrderLockService orderLocks, OrderHoldService holds, Clock clock,
                                 Duration creationGrace) {
        this.payments = payments;
        this.settings = settings;
        this.orderLocks = orderLocks;
        this.holds = holds;
        this.clock = clock;
        this.creationGrace = creationGrace;
    }

    // Paso 1: con el pedido bajo candado (ADR 0016, D.7), revisa que se pueda pagar y guarda el intento.
    // El candado se toma ANTES de revisar: así nadie (el job, un aviso, otro clic) cambia el pedido entre la
    // revisión y el guardado. Quien llegue mientras tanto espera su turno y ve lo que quedó aquí.
    // Las revisiones, en orden:
    //   1. el pedido sigue pendiente de pago y su retención no venció (A.8 y A.9);
    //   2. si ya hay un pago pendiente:
    //      - con enlace y la misma opción: se devuelve ese (el cliente recargó la página), sin llamar a la pasarela;
    //      - con enlace y otra opción: 409 (cambiar de opción llega en el PR 2b, P13);
    //      - sin enlace dentro del plazo de gracia: 409, "se está iniciando" (D.3);
    //      - sin enlace pasado el plazo de gracia: quedó a medio crear; se cierra y se sigue (D.3);
    //   3. el tope de intentos con cobro creado (A.9).
    // Si todo pasa, guarda el intento nuevo, todavía sin cobro, y extiende la retención del pedido.
    public PaymentReservation reserve(UUID orderCode, PaymentMethodPolicy policy) {
        Order order = orderLocks.lockByPublicCode(orderCode);
        Instant now = clock.instant();

        if (order.status() != OrderStatus.PENDING_PAYMENT || !order.holdExpiresAt().isAfter(now)) {
            throw new OrderNotPayableException(orderCode);
        }

        Optional<Payment> pending = payments.findPendingByOrderId(order.id());
        if (pending.isPresent()) {
            Payment current = pending.get();
            if (current.hasCheckout()) {
                if (current.isFor(policy.provider(), policy.method())) {
                    return new PaymentReservation(order, current, true);
                }
                throw new PaymentAlreadyInProgressException();
            }
            // Si devuelve false, sigue dentro del plazo (o su enlace se guardó justo ahora): el cliente espera.
            if (!payments.closeHalfCreatedIfOlderThan(current.id(), now.minus(creationGrace), now)) {
                throw new PaymentStartingException();
            }
        }

        if (payments.countAttemptsWithCheckout(order.id()) >= settings.maxAttemptsPerOrder()) {
            throw new PaymentAttemptsExhaustedException();
        }

        Payment attempt = payments.save(new Payment(null, order.id(), policy.provider(), policy.method(),
                PaymentStatus.PENDING, order.total(), null, null));
        holds.extendHold(order, now.plus(Duration.ofMinutes(policy.holdMinutes())));
        return new PaymentReservation(order, attempt, false);
    }

    // Paso 3, si la pasarela creó el cobro: se anotan su id allá, la URL de pago y hasta cuándo se puede pagar,
    // solo si el intento sigue pendiente (ADR 0016, D.8). Si otro proceso ya lo cerró mientras se esperaba a la pasarela, no se toca nada
    // y el cliente no recibe el enlace: nadie puede pagar ese cobro, que vence solo (D.3).
    public Payment attachCheckout(Payment attempt, CheckoutSession session) {
        Payment attached = attempt.withCheckout(session.providerReference(), session.checkoutUrl());
        if (!payments.attachCheckoutIfPending(attempt.id(), session.providerReference(), session.checkoutUrl(),
                session.expiresAt())) {
            throw new PaymentAttemptClosedException();
        }
        return attached;
    }

    // Paso 3, si la pasarela falló: el intento queda fallido, no a medias, y el pedido puede intentarlo de nuevo.
    // Solo si sigue pendiente (D.8): si otro proceso ya lo cerró, su cierre se respeta, con su motivo y su hora.
    public void markFailed(Payment attempt, PaymentStatusReason reason) {
        payments.markFailedIfPending(attempt.id(), reason, clock.instant());
    }
}
