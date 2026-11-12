package com.floralshop.backend.payment.infra.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;

// Adaptador de salida: lee la política de pago y la traduce al dominio.
@Repository
class PaymentMethodPolicyPersistenceAdapter implements PaymentMethodPolicyRepositoryPort {

    private final PaymentMethodPolicyJpaRepository jpa;

    PaymentMethodPolicyPersistenceAdapter(PaymentMethodPolicyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<PaymentMethodPolicy> findEnabledByPriority() {
        return jpa.findByEnabledTrueOrderByPriorityAscProviderAscMethodAsc().stream()
                .map(PaymentMethodPolicyPersistenceAdapter::toDomain)
                .toList();
    }

    private static PaymentMethodPolicy toDomain(PaymentMethodPolicyJpaEntity row) {
        return new PaymentMethodPolicy(row.getProvider(), PaymentMethod.valueOf(row.getMethod().name()),
                row.isEnabled(), row.getHoldMinutes(), row.getPriority());
    }
}
