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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    private static final Instant NOW = Instant.parse("2026-11-20T15:00:00Z");

    private MercadoPagoGateway gateway(String accessToken) {
        return new MercadoPagoGateway(new MercadoPagoProperties(accessToken, mercadoPago.baseUrl(),
                "http://localhost:3000/pago/resultado", Duration.ofMinutes(15)), Clock.fixed(NOW, ZoneOffset.UTC));
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
        // Vence 15 minutos después de NUESTRA hora al pedirlo (decidido el 2026-11-20).
        assertThat(session.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
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

    // El cuerpo de error de Mercado Pago, con la forma del ejemplo de la referencia: {"errors": [{"code": ...}]}.
    private static String errors(String... codes) {
        return Stream.of(codes).map(code -> "{\"code\":\"" + code + "\",\"message\":\"...\"}")
                .collect(Collectors.joining(",", "{\"errors\":[", "]}"));
    }

    // Clasificación conservadora de los 4xx al crear una order de Checkout Pro (ADR 0016, P5, precisión del
    // 2026-11-20): "rechazada" solo si Mercado Pago dice claramente que la petición está mal o no está autorizada.
    static Stream<Arguments> clientErrors() {
        return Stream.of(
                // Rechazada: sabemos que la order no se creó.
                Arguments.of("400 con un código de petición mal hecha", 400, errors("property_value"), true),
                Arguments.of("400 con varios códigos, todos conocidos", 400,
                        errors("json_syntax_error", "required_properties"), true),
                Arguments.of("401 credenciales de prueba", 401, errors("invalid_credentials"), true),
                Arguments.of("401 token incorrecto (el código es literalmente 401)", 401, errors("401"), true),
                Arguments.of("403 sin permisos", 403, errors("forbidden"), true),
                Arguments.of("422 no se puede procesar", 422, errors("unprocessable_entity"), true),
                // Sin confirmar: no podemos afirmar que no se creó.
                Arguments.of("400 falla de idempotencia", 400, errors("idempotency_validation_failed"), false),
                Arguments.of("400 con un código conocido y uno de idempotencia", 400,
                        errors("property_value", "idempotency_validation_failed"), false),
                Arguments.of("400 con un código que la referencia no tiene", 400, errors("some_new_code"), false),
                Arguments.of("400 con la lista de errores vacía", 400, "{\"errors\":[]}", false),
                Arguments.of("400 con un cuerpo que no es JSON", 400, "<html>Bad request</html>", false),
                Arguments.of("400 sin cuerpo", 400, "", false),
                Arguments.of("400 sin el campo errors", 400, "{}", false),
                Arguments.of("400 con un error sin code", 400, "{\"errors\":[{\"message\":\"...\"}]}", false),
                // La misma regla para 401, 403 y 422: sin códigos conocidos no es "rechazada".
                Arguments.of("401 con un código que la referencia no tiene", 401, errors("some_new_code"), false),
                Arguments.of("401 sin cuerpo", 401, "", false),
                Arguments.of("401 con un cuerpo que no es JSON", 401, "<html>Unauthorized</html>", false),
                Arguments.of("403 con un código que la referencia no tiene", 403, errors("some_new_code"), false),
                Arguments.of("403 sin cuerpo", 403, "", false),
                Arguments.of("422 con un código que la referencia no tiene", 422, errors("some_new_code"), false),
                Arguments.of("422 sin cuerpo", 422, "", false),
                // Un código de otro HTTP no sirve: cada HTTP tiene su propia lista.
                Arguments.of("422 con un código que solo vale para el 400", 422, errors("property_value"), false),
                Arguments.of("409 llave ya usada", 409, errors("idempotency_key_already_used"), false),
                Arguments.of("423 llave bloqueada", 423, errors("resource_locked"), false),
                Arguments.of("429 demasiadas peticiones", 429, errors("too_many_requests"), false),
                Arguments.of("404 que la referencia no menciona", 404, "", false),
                Arguments.of("408 tiempo agotado del lado de Mercado Pago", 408, "", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientErrors")
    void aClientErrorIsRejectedOnlyWhenMercadoPagoClearlySaysTheRequestIsWrong(
            String scenario, int status, String body, boolean rejected) {
        mercadoPago.stubFor(post("/v1/orders").willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(body)));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOfSatisfying(PaymentGatewayException.class,
                        ex -> assertThat(ex.rejected()).isEqualTo(rejected));
    }

    // 5xx: algo falló allá; no sabemos si lo creó. No es un rechazo (D.3).
    @Test
    void aServerErrorIsAnUnconfirmedGatewayError() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOfSatisfying(PaymentGatewayException.class, ex -> assertThat(ex.rejected()).isFalse());
    }

    // Si no llega respuesta (aquí, la conexión se corta), tampoco sabemos: no es un rechazo.
    @Test
    void aBrokenConnectionIsAnUnconfirmedGatewayError() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOfSatisfying(PaymentGatewayException.class, ex -> assertThat(ex.rejected()).isFalse());
    }

    @Test
    void anAnswerWithoutCheckoutUrlIsAnUnconfirmedGatewayError() {
        mercadoPago.stubFor(post("/v1/orders").willReturn(okJson("{\"id\": \"ORDTST01ABC\"}")));

        assertThatThrownBy(() -> gateway("APP_USR-123").createCheckout(card("145000")))
                .isInstanceOfSatisfying(PaymentGatewayException.class, ex -> assertThat(ex.rejected()).isFalse());
    }

    // En pesos, Mercado Pago solo acepta montos enteros. Unos centavos son un error, no se redondean.
    @Test
    void amountsAreSentInWholePesos() {
        assertThat(MercadoPagoGateway.pesos(new BigDecimal("145000.00"))).isEqualTo("145000");
        assertThatThrownBy(() -> MercadoPagoGateway.pesos(new BigDecimal("145000.50")))
                .isInstanceOf(ArithmeticException.class);
    }
}
