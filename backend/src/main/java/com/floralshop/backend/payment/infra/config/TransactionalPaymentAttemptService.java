package com.floralshop.backend.payment.infra.config;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.application.service.OrderLockService;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentSettingsRepositoryPort;
import com.floralshop.backend.payment.application.service.PaymentAttemptService;
import com.floralshop.backend.payment.application.service.PaymentReservation;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Misma lógica que PaymentAttemptService, con una transacción corta por paso (ADR 0011).
// En reserve, el candado del pedido dura toda la transacción: revisar, cerrar el intento a medio crear,
// guardar el intento nuevo y extender la franja van juntos, o ninguno. Si una revisión falla, no queda nada.
// No añade lógica.
class TransactionalPaymentAttemptService extends PaymentAttemptService {

    TransactionalPaymentAttemptService(PaymentRepositoryPort payments, PaymentSettingsRepositoryPort settings,
                                       OrderLockService orderLocks, OrderHoldService holds, Clock clock,
                                       Duration creationGrace) {
        super(payments, settings, orderLocks, holds, clock, creationGrace);
    }

    @Override
    @Transactional
    public PaymentReservation reserve(UUID orderCode, PaymentMethodPolicy policy) {
        return super.reserve(orderCode, policy);
    }

    @Override
    @Transactional
    public Payment attachCheckout(Payment attempt, CheckoutSession session) {
        return super.attachCheckout(attempt, session);
    }

    @Override
    @Transactional
    public void markFailed(Payment attempt, PaymentStatusReason reason) {
        super.markFailed(attempt, reason);
    }
}
