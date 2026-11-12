package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// Sin credencial, Mercado Pago no se ofrece (ADR 0014, punto 2).
class MercadoPagoGatewayTest {

    @Test
    void isConfiguredOnlyWithANonBlankAccessToken() {
        assertThat(new MercadoPagoGateway("TEST-123").isConfigured()).isTrue();
        assertThat(new MercadoPagoGateway("").isConfigured()).isFalse();
        assertThat(new MercadoPagoGateway("   ").isConfigured()).isFalse();
        assertThat(new MercadoPagoGateway(null).isConfigured()).isFalse();
    }
}
