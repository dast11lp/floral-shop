package com.floralshop.backend.payment.application.port.out;

import java.util.List;
import java.util.Optional;

import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;

// Puerto de salida: la política de pago por pasarela y medio.
public interface PaymentMethodPolicyRepositoryPort {

    // Solo las habilitadas, de la más prioritaria (número menor) a la menos.
    List<PaymentMethodPolicy> findEnabledByPriority();

    // La combinación si existe y está habilitada.
    Optional<PaymentMethodPolicy> findEnabled(String provider, PaymentMethod method);
}
