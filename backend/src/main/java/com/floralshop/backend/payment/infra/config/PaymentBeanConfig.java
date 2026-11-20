package com.floralshop.backend.payment.infra.config;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.application.service.OrderLockService;
import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentSettingsRepositoryPort;
import com.floralshop.backend.payment.application.service.PaymentAttemptService;
import com.floralshop.backend.payment.application.service.PaymentGatewayRegistry;
import com.floralshop.backend.payment.application.service.PaymentOptionsService;
import com.floralshop.backend.payment.application.service.StartPaymentService;
import com.floralshop.backend.payment.infra.adapter.out.mercadopago.MercadoPagoProperties;

@Configuration
@EnableConfigurationProperties({MercadoPagoProperties.class, PaymentAttemptProperties.class})
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

    // Cada paso con su propia transacción corta (TransactionalPaymentAttemptService).
    @Bean
    PaymentAttemptService paymentAttemptService(PaymentRepositoryPort payments,
                                                PaymentSettingsRepositoryPort settings, OrderLockService orderLocks,
                                                OrderHoldService holds, Clock clock,
                                                PaymentAttemptProperties properties) {
        return new TransactionalPaymentAttemptService(payments, settings, orderLocks, holds, clock,
                properties.creationGrace());
    }

    // Sin transacción propia: coordina los pasos, y la llamada a la pasarela queda fuera de toda transacción.
    @Bean
    StartPaymentService startPaymentService(PaymentMethodPolicyRepositoryPort policies,
                                            PaymentGatewayRegistry gateways, PaymentAttemptService attempts) {
        return new StartPaymentService(policies, gateways, attempts);
    }
}
