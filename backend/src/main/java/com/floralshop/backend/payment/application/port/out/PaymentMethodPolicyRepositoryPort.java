package com.floralshop.backend.payment.application.port.out;

import java.util.List;

import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;

// Puerto de salida: la política de pago por pasarela y medio.
public interface PaymentMethodPolicyRepositoryPort {

    // Solo las habilitadas, de la más prioritaria (número menor) a la menos.
    List<PaymentMethodPolicy> findEnabledByPriority();
}
