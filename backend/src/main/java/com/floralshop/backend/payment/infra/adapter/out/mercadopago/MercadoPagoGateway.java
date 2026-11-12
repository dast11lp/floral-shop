package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.domain.exception.PaymentGatewayException;
import com.floralshop.backend.payment.domain.model.PaymentMethod;

// Adaptador de Mercado Pago: Checkout Pro vía la API de Orders (ADR 0003 y 0014).
// Crea una "order" y devuelve su checkout_url, la página de Mercado Pago donde paga el cliente.
// Capa anticorrupción: el vocabulario de Mercado Pago (order, not_allowed_types...) no sale de esta clase.
// La credencial nunca se escribe en el log ni sale en una respuesta.
@Component
class MercadoPagoGateway implements PaymentGateway {

    static final String PROVIDER = "mercadopago";

    // Qué tipos de Mercado Pago se excluyen para que el cliente solo vea el medio que eligió.
    // Los tipos salen de GET /v1/payment_methods de la cuenta, y Mercado Pago aceptó las dos listas
    // en el sandbox (consultado el 2026-11-12).
    // Efecty (ticket) y el dinero en cuenta siempre quedan fuera (ADR 0012).
    private static final Map<PaymentMethod, List<String>> EXCLUDED_TYPES = Map.of(
            PaymentMethod.CARD, List.of("bank_transfer", "ticket", "account_money"),
            PaymentMethod.PSE, List.of("credit_card", "debit_card", "prepaid_card", "ticket", "account_money"));

    private final MercadoPagoProperties properties;
    private final RestClient http;

    MercadoPagoGateway(MercadoPagoProperties properties) {
        this.properties = properties;
        // HTTP/1.1 y tiempos máximos: si Mercado Pago no responde, el cliente recibe un error en segundos,
        // sin dejar la petición colgada. (Con HTTP/2 sin TLS, el cliente de Java falla contra WireMock.)
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasAccessToken();
    }

    @Override
    public CheckoutSession createCheckout(CheckoutRequest request) {
        List<String> excluded = EXCLUDED_TYPES.get(request.method());
        if (excluded == null) {
            throw new IllegalArgumentException("Mercado Pago no maneja el medio " + request.method());
        }
        String amount = pesos(request.amount());
        String returnUrl = properties.returnUrl();

        Map<String, Object> body = Map.of(
                "type", "online",
                "processing_mode", "manual", // el único valor que acepta Checkout Pro
                "external_reference", request.reference(),
                "total_amount", amount,
                "expiration_time", properties.checkoutExpiration().toString(), // ISO 8601, por ejemplo PT15M
                "items", List.of(Map.of("title", request.description(), "unit_price", amount, "quantity", 1)),
                "config", Map.of(
                        "payment_method", Map.of("not_allowed_types", excluded),
                        "online", Map.of("success_url", returnUrl, "failure_url", returnUrl,
                                "pending_url", returnUrl)));

        try {
            OrderResponse response = http.post()
                    .uri("/v1/orders")
                    .header("Authorization", "Bearer " + properties.accessToken())
                    // Una llave por intento de pago: si la misma petición se repite por un fallo de red,
                    // Mercado Pago devuelve el mismo cobro en vez de crear otro.
                    .header("X-Idempotency-Key", request.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OrderResponse.class);
            if (response == null || response.id() == null || response.checkoutUrl() == null) {
                throw new PaymentGatewayException("Mercado Pago respondió sin id o sin checkout_url", null);
            }
            return new CheckoutSession(response.id(), response.checkoutUrl());
        } catch (RestClientException ex) {
            throw new PaymentGatewayException("Mercado Pago no pudo crear el cobro: " + ex.getMessage(), ex);
        }
    }

    // En pesos colombianos, Mercado Pago solo acepta montos enteros ("145000", no "145000.00").
    // Comprobado en el sandbox el 2026-11-12. Si el monto tuviera centavos, es un error: se avisa en vez de redondear.
    static String pesos(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
    }

    // Solo los campos que se usan de la respuesta de POST /v1/orders.
    record OrderResponse(String id, @JsonProperty("checkout_url") String checkoutUrl) {
    }
}
