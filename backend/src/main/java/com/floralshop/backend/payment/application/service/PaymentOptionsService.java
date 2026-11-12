package com.floralshop.backend.payment.application.service;

import java.util.ArrayList;
import java.util.List;

import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentOption;

// Las opciones de pago que ve el cliente (ADR 0014, punto 1). Java puro, sin anotaciones de Spring.
public class PaymentOptionsService {

    private final PaymentMethodPolicyRepositoryPort policies;
    private final PaymentGatewayRegistry gateways;

    public PaymentOptionsService(PaymentMethodPolicyRepositoryPort policies, PaymentGatewayRegistry gateways) {
        this.policies = policies;
        this.gateways = gateways;
    }

    // Las habilitadas cuya pasarela tiene credenciales, por prioridad.
    // La primera que queda es la sugerida: si se apaga la sugerida, la siguiente toma su lugar sola.
    public List<PaymentOption> listOptions() {
        List<PaymentOption> options = new ArrayList<>();
        for (PaymentMethodPolicy policy : policies.findEnabledByPriority()) {
            if (gateways.isAvailable(policy.provider())) {
                options.add(new PaymentOption(policy.provider(), policy.method(), options.isEmpty()));
            }
        }
        return options;
    }
}
