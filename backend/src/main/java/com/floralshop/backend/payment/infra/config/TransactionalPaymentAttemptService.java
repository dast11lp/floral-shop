package com.floralshop.backend.payment.infra.config;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.application.service.PaymentAttemptService;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;

// Misma lógica que PaymentAttemptService, con una transacción corta por paso (ADR 0011).
// En reserve, guardar el intento y extender la franja del pedido van juntos: o los dos, o ninguno.
// No añade lógica.
class TransactionalPaymentAttemptService extends PaymentAttemptService {

    TransactionalPaymentAttemptService(PaymentRepositoryPort payments, OrderHoldService holds, Clock clock) {
        super(payments, holds, clock);
    }

    @Override
    @Transactional
    public Payment reserve(Order order, PaymentMethodPolicy policy) {
        return super.reserve(order, policy);
    }

    @Override
    @Transactional
    public Payment attachCheckout(Payment attempt, CheckoutSession session) {
        return super.attachCheckout(attempt, session);
    }

    @Override
    @Transactional
    public void markFailed(Payment attempt) {
        super.markFailed(attempt);
    }
}
