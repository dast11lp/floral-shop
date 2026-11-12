package com.floralshop.backend.payment.infra.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.application.service.PaymentGatewayRegistry;
import com.floralshop.backend.payment.application.service.PaymentOptionsService;

@Configuration
class PaymentBeanConfig {

    // Spring entrega todos los adaptadores de PaymentGateway que existan: uno por pasarela.
    @Bean
    PaymentGatewayRegistry paymentGatewayRegistry(List<PaymentGateway> gateways) {
        return new PaymentGatewayRegistry(gateways);
    }

    @Bean
    PaymentOptionsService paymentOptionsService(PaymentMethodPolicyRepositoryPort policies,
                                                PaymentGatewayRegistry gateways) {
        return new PaymentOptionsService(policies, gateways);
    }
}
