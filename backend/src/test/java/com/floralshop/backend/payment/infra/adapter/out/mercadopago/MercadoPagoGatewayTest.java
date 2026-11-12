package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.domain.exception.PaymentGatewayException;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

// El adaptador de Mercado Pago contra una API simulada con WireMock: qué envía y cómo lee la respuesta.
// La forma de la petición y de la respuesta se comprobó contra el sandbox real el 2026-11-12.
class MercadoPagoGatewayTest {

    @RegisterExtension
    static WireMockExtension mercadoPago = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private MercadoPagoGateway gateway(String accessToken) {
        return new MercadoPagoGateway(new MercadoPagoProperties(accessToken, mercadoPago.baseUrl(),
                "http://localhost:3000/pago/resultado", Duration.ofMinutes(15)));
    }

    private static CheckoutRequest card(String amount) {
        return new CheckoutRequest("7f2c-pedido", "Pedido 7f2c-pedido", new BigDecimal(amount), PaymentMethod.CARD,
                "7f2c-pedido-7");
    }

    @Test
    void isConfiguredOnlyWithANonBlankAccessToken() {
        assertThat(gateway("APP_USR-123").isConfigured()).isTrue();
        assertThat(gateway("").isConfigured()).isFalse();
        assertThat(gateway("   ").isConfigured()).isFalse();
        assertThat(gateway(null).isConfigured()).isFalse();
    }

    @Test
    void createsACheckoutProOrderForCardOnly() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(okJson("""
                {"id": "ORDTST01ABC", "status": "created",
                 "checkout_url": "https://www.mercadopago.com.co/checkout/v1/redirect?order_id=ORDTST01ABC"}
                """).withStatus(201)));

        CheckoutSession session = gateway("APP_USR-123").createCheckout(card("145000.00"));

        assertThat(session.providerReference()).isEqualTo("ORDTST01ABC");
        assertThat(session.checkoutUrl()).endsWith("order_id=ORDTST01ABC");
        mercadoPago.verify(postRequestedFor(urlEqualTo("/v1/orders"))
                .withHeader("Authorization", equalTo("Bearer APP_USR-123"))
                // La llave la decide el caso de uso (pedido + intento); el adaptador la envía tal cual.
                .withHeader("X-Idempotency-Key", equalTo("7f2c-pedido-7"))
                .withRequestBody(equalToJson("""
                        {"type": "online", "processing_mode": "manual",
                         "external_reference": "7f2c-pedido",
                         "total_amount": "145000", "expiration_time": "PT15M",
                         "items": [{"title": "Pedido 7f2c-pedido", "unit_price": "145000", "quantity": 1}],
                         "config": {
                           "payment_method": {"not_allowed_types": ["bank_transfer", "ticket", "account_money"]},
                           "online": {"success_url": "http://localhost:3000/pago/resultado",
                                      "failure_url": "http://localhost:3000/pago/resultado",
                                      "pending_url": "http://localhost:3000/pago/resultado"}}}
                        """)));
    }

    @Test
    void aRejectedRequestBecomesAGatewayError() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(aResponse().withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"errors\":[{\"code\":\"property_value\"}]}")));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    void anAnswerWithoutCheckoutUrlIsAGatewayError() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(okJson("{\"id\": \"ORDTST01ABC\"}")));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOf(PaymentGatewayException.class);
    }

    // En pesos, Mercado Pago solo acepta montos enteros. Unos centavos son un error, no se redondean.
    @Test
    void amountsAreSentInWholePesos() {
        assertThat(MercadoPagoGateway.pesos(new BigDecimal("145000.00"))).isEqualTo("145000");
        assertThatThrownBy(() -> MercadoPagoGateway.pesos(new BigDecimal("145000.50")))
                .isInstanceOf(ArithmeticException.class);
    }
}
