package com.floralshop.backend.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentOption;

// Las opciones de pago: habilitadas, con pasarela disponible, por prioridad y con la primera sugerida.
@ExtendWith(MockitoExtension.class)
class PaymentOptionsServiceTest {

    @Mock
    PaymentMethodPolicyRepositoryPort policies;

    private static PaymentMethodPolicy policy(String provider, PaymentMethod method, int priority) {
        return new PaymentMethodPolicy(provider, method, true, 36, priority);
    }

    // Una pasarela de mentira: solo importa su nombre y si tiene credenciales.
    private static PaymentGateway gateway(String provider, boolean configured) {
        return new PaymentGateway() {
            @Override
            public String provider() { return provider; }

            @Override
            public boolean isConfigured() { return configured; }

            @Override
            public CheckoutSession createCheckout(CheckoutRequest request) {
                throw new UnsupportedOperationException("No se usa en estas pruebas");
            }
        };
    }

    private PaymentOptionsService service(PaymentGateway... gateways) {
        return new PaymentOptionsService(policies, new PaymentGatewayRegistry(List.of(gateways)));
    }

    @Test
    void theFirstOptionIsSuggestedAndTheRestKeepTheirOrder() {
        when(policies.findEnabledByPriority()).thenReturn(List.of(
                policy("mercadopago", PaymentMethod.CARD, 1),
                policy("mercadopago", PaymentMethod.PSE, 2),
                policy("wompi", PaymentMethod.NEQUI, 3)));

        List<PaymentOption> options = service(gateway("mercadopago", true), gateway("wompi", true)).listOptions();

        assertThat(options).containsExactly(
                new PaymentOption("mercadopago", PaymentMethod.CARD, true),
                new PaymentOption("mercadopago", PaymentMethod.PSE, false),
                new PaymentOption("wompi", PaymentMethod.NEQUI, false));
    }

    @Test
    void aGatewayWithoutCredentialsIsNotOfferedAndTheNextOneBecomesSuggested() {
        when(policies.findEnabledByPriority()).thenReturn(List.of(
                policy("wompi", PaymentMethod.NEQUI, 1),
                policy("mercadopago", PaymentMethod.CARD, 2)));

        List<PaymentOption> options = service(gateway("wompi", false), gateway("mercadopago", true)).listOptions();

        assertThat(options).containsExactly(new PaymentOption("mercadopago", PaymentMethod.CARD, true));
    }

    @Test
    void aPolicyForAGatewayWithoutCodeIsNotOffered() {
        when(policies.findEnabledByPriority()).thenReturn(List.of(policy("wompi", PaymentMethod.NEQUI, 1)));

        assertThat(service(gateway("mercadopago", true)).listOptions()).isEmpty();
    }

    @Test
    void twoGatewaysWithTheSameNameAreAConfigurationError() {
        assertThatThrownBy(() -> new PaymentGatewayRegistry(
                List.of(gateway("mercadopago", true), gateway("mercadopago", true))))
                .isInstanceOf(IllegalStateException.class);
    }
}
