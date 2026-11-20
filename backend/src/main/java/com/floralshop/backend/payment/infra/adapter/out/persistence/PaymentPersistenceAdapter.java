package com.floralshop.backend.payment.infra.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentStatus;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Adaptador de salida: guarda y busca pagos con JPA. La transacción la abre el caso de uso (infra/config).
@Repository
class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    // El índice único de la migración V8: un pedido no puede tener dos pagos pendientes.
    private static final String ONE_PENDING_PER_ORDER = "uq_payment_one_pending_per_order";

    private final PaymentJpaRepository jpa;

    PaymentPersistenceAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    // Se escribe en la base en el acto (saveAndFlush) para que el índice único actúe dentro de esta misma
    // transacción. Si dos peticiones intentan pagar el mismo pedido a la vez, la segunda choca aquí, antes de
    // llamar a la pasarela, y recibe "ya hay un pago en curso" en vez de un error interno.
    @Override
    public Payment save(Payment payment) {
        try {
            return toDomain(jpa.saveAndFlush(toEntity(payment)));
        } catch (DataIntegrityViolationException ex) {
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(ONE_PENDING_PER_ORDER)) {
                throw new PaymentAlreadyInProgressException();
            }
            throw ex;
        }
    }

    @Override
    public Optional<Payment> findPendingByOrderId(long orderId) {
        return jpa.findByOrderIdAndStatus(orderId, PaymentStatusColumn.PENDING).map(PaymentPersistenceAdapter::toDomain);
    }

    @Override
    public int countAttemptsWithCheckout(long orderId) {
        return Math.toIntExact(jpa.countWithReference(orderId));
    }

    @Override
    public boolean closeHalfCreatedIfOlderThan(long paymentId, Instant createdBefore, Instant resolvedAt) {
        return jpa.closeWithoutReferenceIfOlderThan(paymentId, PaymentStatusColumn.PENDING, createdBefore,
                PaymentStatusColumn.FAILED, PaymentStatusReasonColumn.CREATION_UNCONFIRMED, resolvedAt) == 1;
    }

    @Override
    public boolean attachCheckoutIfPending(long paymentId, String providerReference, String checkoutUrl) {
        return jpa.attachCheckoutIf(paymentId, PaymentStatusColumn.PENDING, providerReference, checkoutUrl) == 1;
    }

    @Override
    public boolean markFailedIfPending(long paymentId, PaymentStatusReason reason, Instant resolvedAt) {
        return jpa.closeIf(paymentId, PaymentStatusColumn.PENDING, PaymentStatusColumn.FAILED,
                PaymentStatusReasonColumn.valueOf(reason.name()), resolvedAt) == 1;
    }

    private static PaymentJpaEntity toEntity(Payment p) {
        return new PaymentJpaEntity(p.id(), p.orderId(), p.provider(), PaymentMethodColumn.valueOf(p.method().name()),
                PaymentStatusColumn.valueOf(p.status().name()), p.amount(), p.providerReference(), p.checkoutUrl());
    }

    private static Payment toDomain(PaymentJpaEntity e) {
        return new Payment(e.getId(), e.getOrderId(), e.getProvider(), PaymentMethod.valueOf(e.getMethod().name()),
                PaymentStatus.valueOf(e.getStatus().name()), e.getAmount(), e.getProviderReference(),
                e.getCheckoutUrl());
    }
}
