package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
    private final Clock clock;
    private final RestClient http;

    MercadoPagoGateway(MercadoPagoProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
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

        // El vencimiento se cuenta con NUESTRO reloj, tomado justo antes de pedir el cobro (decidido el
        // 2026-11-20). Queda unos segundos antes que el de Mercado Pago: al cliente nunca se le dice una hora
        // que ya pasó, y el job espera un margen después del vencimiento (ADR 0016, A.19).
        Instant expiresAt = clock.instant().plus(properties.checkoutExpiration());
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
            return new CheckoutSession(response.id(), response.checkoutUrl(), expiresAt);
        } catch (HttpClientErrorException ex) {
            // Un 4xx no basta para saber que la order no existe: ver surelyNotCreated (ADR 0016, P5).
            List<String> codes = errorCodes(ex);
            boolean rejected = surelyNotCreated(ex.getStatusCode().value(), codes);
            throw new PaymentGatewayException((rejected ? "Mercado Pago rechazó el cobro: "
                    : "Mercado Pago no confirmó si creó el cobro: ") + ex.getStatusCode() + " " + codes, ex, rejected);
        } catch (RestClientException ex) {
            // No respondió, se agotó el tiempo o un 5xx: no sabemos si lo creó (D.3).
            throw new PaymentGatewayException("Mercado Pago no pudo crear el cobro: " + ex.getMessage(), ex);
        }
    }

    // Por cada HTTP reconocido, los códigos de error que, según la referencia de POST /v1/orders en Checkout Pro
    // (consultada el 2026-11-20), dicen que la petición está mal hecha o no está autorizada: Mercado Pago no
    // creó la order. El 400 no incluye idempotency_validation_failed, que pide repetir la petición y no dice
    // si la order se creó.
    private static final Map<Integer, Set<String>> REJECTION_CODES = Map.of(
            400, Set.of("json_syntax_error", "required_properties", "unsupported_properties", "minimum_properties",
                    "property_type", "property_value", "order_items_total_amount_mismatch", "invalid_total_amount",
                    "empty_required_header", "invalid_idempotency_key_length"),
            401, Set.of("401", "invalid_credentials"),
            403, Set.of("forbidden", "pa_unauthorized_result_from_policies"),
            422, Set.of("unprocessable_entity"));

    // Clasificación conservadora, SOLO para crear una order de Checkout Pro (ADR 0016, P5, precisión del
    // 2026-11-20). Una sola regla: "rechazada" (true) solo si el HTTP está reconocido, hay al menos un código
    // de error y TODOS los códigos están en la lista de ese HTTP. Cualquier otra cosa es "sin confirmar"
    // (false): un código desconocido o sin valor, un cuerpo vacío o ilegible, 409 y 423 (la order pudo
    // crearse con esa llave), 429 y cualquier otro 4xx.
    static boolean surelyNotCreated(int status, List<String> codes) {
        Set<String> known = REJECTION_CODES.get(status);
        return known != null && !codes.isEmpty()
                && codes.stream().allMatch(code -> code != null && known.contains(code));
    }

    // Los códigos de la respuesta de error: {"errors": [{"code": "...", "message": "..."}]}.
    // Si el cuerpo está vacío o no se puede leer, no hay códigos, y eso nunca lleva a "rechazada".
    private static List<String> errorCodes(HttpClientErrorException ex) {
        try {
            ErrorResponse body = ex.getResponseBodyAs(ErrorResponse.class);
            if (body == null || body.errors() == null) {
                return List.of();
            }
            return body.errors().stream().map(error -> error == null ? null : error.code()).toList();
        } catch (RuntimeException unreadable) {
            return List.of();
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

    // Solo el código de cada error de la respuesta de error.
    record ErrorResponse(List<ErrorItem> errors) {
    }

    record ErrorItem(String code) {
    }
}
