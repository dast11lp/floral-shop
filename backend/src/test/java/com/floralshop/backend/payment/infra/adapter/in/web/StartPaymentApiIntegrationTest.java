package com.floralshop.backend.payment.infra.adapter.in.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentStatus;
import com.jayway.jsonpath.JsonPath;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

// Punta a punta: Postgres real (Testcontainers) y Mercado Pago simulado con WireMock.
// Se crea un pedido de verdad con POST /api/v1/orders y luego se inicia su pago.
// Sin @Transactional: cada paso abre su propia transacción, como en producción.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StartPaymentApiIntegrationTest {

    private static final String CHECKOUT_URL =
            "https://www.mercadopago.com.co/checkout/v1/redirect?order_id=ORDTST01ABC";

    @RegisterExtension
    static WireMockExtension mercadoPago = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void mercadoPagoProperties(DynamicPropertyRegistry registry) {
        registry.add("payment.mercadopago.base-url", mercadoPago::baseUrl);
        registry.add("payment.mercadopago.access-token", () -> "APP_USR-credencial-inventada");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PaymentRepositoryPort payments;

    private final LocalDate inFiveDays = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(5);

    private String orderCode;

    @BeforeEach
    void setUp() throws Exception {
        clean();
        long zoneId = jdbc.queryForObject("insert into delivery_zone (name, shipping_fee, travel_margin_hours) "
                + "values ('Zona pagos', 15000, 1) returning id", Long.class);
        long productId = jdbc.queryForObject("insert into product (name, slug, crafting_hours) "
                + "values ('Ramo de pagos', 'ramo-pagos', 24) returning id", Long.class);
        long variantId = jdbc.queryForObject("insert into product_variant (product_id, name, price) "
                + "values (?, 'Mediano', 130000) returning id", Long.class, productId);
        long slotId = jdbc.queryForObject("insert into delivery_slot (slot_date, start_time, end_time, capacity, booked) "
                + "values (?, '13:00', '15:00', 4, 0) returning id", Long.class, inFiveDays);

        String body = """
                {"customer": {"name": "Ana", "email": "ana@example.com", "phone": "3000000000"},
                 "deliveryAddress": "Calle 1 # 2-3", "zoneId": %d, "slotId": %d,
                 "lines": [{"variantId": %d, "quantity": 1}]}
                """.formatted(zoneId, slotId, variantId);
        String response = mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        orderCode = JsonPath.read(response, "$.code");

        mercadoPagoAnswers(0);
    }

    @AfterEach
    void clean() {
        mercadoPago.resetAll();
        jdbc.update("update payment_method_policy set enabled = false where provider = 'mercadopago' and method = 'PSE'");
        jdbc.update("delete from payment");
        jdbc.update("delete from order_line");
        jdbc.update("delete from customer_order");
        jdbc.update("delete from product_variant where product_id in (select id from product where slug = 'ramo-pagos')");
        jdbc.update("delete from product where slug = 'ramo-pagos'");
        jdbc.update("delete from delivery_zone where name = 'Zona pagos'");
        jdbc.update("delete from delivery_slot where slot_date = ? and start_time = '13:00'", inFiveDays);
    }

    // Mercado Pago crea el cobro, tardando los milisegundos indicados en responder.
    private static void mercadoPagoAnswers(int delayMillis) {
        mercadoPago.stubFor(WireMock.post("/v1/orders").willReturn(okJson("""
                {"id": "ORDTST01ABC", "status": "created", "checkout_url": "%s"}
                """.formatted(CHECKOUT_URL)).withStatus(201).withFixedDelay(delayMillis)));
    }

    private ResultActions pay(String code, String method) throws Exception {
        return mockMvc.perform(post("/api/v1/orders/{code}/payments", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\": \"mercadopago\", \"method\": \"" + method + "\"}"));
    }

    private int paymentCount() {
        return jdbc.queryForObject("select count(*) from payment", Integer.class);
    }

    private long orderId() {
        return jdbc.queryForObject("select id from customer_order where public_code = ?::uuid", Long.class, orderCode);
    }

    private double holdMinutesAfterCreation() {
        return jdbc.queryForObject("select extract(epoch from hold_expires_at - created_at) / 60 from customer_order",
                Double.class);
    }

    @Test
    void startsTheCardPaymentSavesItPendingAndExtendsTheOrderHold() throws Exception {
        assertThat(holdMinutesAfterCreation()).isBetween(14.9, 15.1);

        pay(orderCode, "CARD")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.amount").value(145000))
                .andExpect(jsonPath("$.checkoutUrl").value(CHECKOUT_URL));

        Map<String, Object> row = jdbc.queryForMap("select id, status, provider_reference, checkout_url from payment");
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("provider_reference")).isEqualTo("ORDTST01ABC");
        assertThat(row.get("checkout_url")).isEqualTo(CHECKOUT_URL);
        // La franja ahora vence 36 minutos después (la tarjeta en payment_method_policy), dentro del tope de 51.
        assertThat(holdMinutesAfterCreation()).isBetween(35.9, 36.2);
        // La llave de idempotencia: código del pedido + id del intento.
        mercadoPago.verify(postRequestedFor(urlEqualTo("/v1/orders"))
                .withHeader("X-Idempotency-Key", equalTo(orderCode + "-" + row.get("id"))));
    }

    @Test
    void askingAgainReturnsTheSameCheckoutWithoutCreatingAnother() throws Exception {
        pay(orderCode, "CARD").andExpect(status().isCreated());
        pay(orderCode, "CARD").andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkoutUrl").value(endsWith("ORDTST01ABC")));

        mercadoPago.verify(1, postRequestedFor(urlEqualTo("/v1/orders")));
        assertThat(paymentCount()).isEqualTo(1);
    }

    // Cambiar de opción con un pago en curso llega en el PR 2. Mientras tanto: 409 con un mensaje claro.
    @Test
    void switchingOptionWhileAPaymentIsInProgressIsAConflictWithAClearMessage() throws Exception {
        jdbc.update("update payment_method_policy set enabled = true where provider = 'mercadopago' and method = 'PSE'");
        pay(orderCode, "CARD").andExpect(status().isCreated());

        pay(orderCode, "PSE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("ya tiene un pago en curso")));

        mercadoPago.verify(1, postRequestedFor(urlEqualTo("/v1/orders")));
        assertThat(paymentCount()).isEqualTo(1);
    }

    // PSE existe en la política, pero arranca apagado (migración V7).
    @Test
    void aDisabledOptionIsRejected() throws Exception {
        pay(orderCode, "PSE").andExpect(status().isUnprocessableContent());
        mercadoPago.verify(0, postRequestedFor(urlEqualTo("/v1/orders")));
    }

    @Test
    void anUnknownOrderIsNotFound() throws Exception {
        pay("00000000-0000-4000-8000-000000000000", "CARD").andExpect(status().isNotFound());
    }

    // Si Mercado Pago falla, el intento queda FAILED (no pendiente a medias) y el cliente puede reintentar.
    // El reintento usa otra llave de idempotencia: es otro intento.
    @Test
    void aGatewayFailureLeavesTheAttemptFailedAndARetryIsAllowed() throws Exception {
        mercadoPago.stubFor(WireMock.post("/v1/orders").willReturn(aResponse().withStatus(500)));

        pay(orderCode, "CARD").andExpect(status().isBadGateway());
        assertThat(jdbc.queryForObject("select status from payment", String.class)).isEqualTo("FAILED");

        mercadoPagoAnswers(0);
        pay(orderCode, "CARD").andExpect(status().isCreated());

        List<String> statuses = jdbc.queryForList("select status from payment order by id", String.class);
        assertThat(statuses).containsExactly("FAILED", "PENDING");
        List<Long> ids = jdbc.queryForList("select id from payment order by id", Long.class);
        mercadoPago.verify(postRequestedFor(urlEqualTo("/v1/orders"))
                .withHeader("X-Idempotency-Key", equalTo(orderCode + "-" + ids.get(0))));
        mercadoPago.verify(postRequestedFor(urlEqualTo("/v1/orders"))
                .withHeader("X-Idempotency-Key", equalTo(orderCode + "-" + ids.get(1))));
    }

    // Mientras se espera a Mercado Pago no hay ninguna transacción abierta: el intento ya está guardado
    // (otra conexión lo ve, todavía sin cobro), la franja ya está extendida, y Postgres no tiene
    // ninguna sesión de esta base "en transacción y esperando".
    @Test
    void theGatewayIsCalledOutsideAnyDatabaseTransaction() throws Exception {
        mercadoPagoAnswers(2000);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> request = executor.submit(() ->
                    pay(orderCode, "CARD").andReturn().getResponse().getStatus());

            // El intento ya se guardó; Mercado Pago tarda 2 s, así que 300 ms después la llamada sigue en curso.
            waitUntil(() -> paymentCount() == 1);
            Thread.sleep(300);

            Map<String, Object> row = jdbc.queryForMap("select status, provider_reference from payment");
            assertThat(row.get("status")).isEqualTo("PENDING");
            assertThat(row.get("provider_reference")).isNull();
            assertThat(holdMinutesAfterCreation()).isBetween(35.9, 36.2);
            assertThat(jdbc.queryForObject("select count(*) from pg_stat_activity where datname = current_database() "
                    + "and state like 'idle in transaction%'", Integer.class)).isZero();

            assertThat(request.get(10, TimeUnit.SECONDS)).isEqualTo(201);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select provider_reference from payment", String.class))
                .isEqualTo("ORDTST01ABC");
    }

    // Dos peticiones a la vez para el mismo pedido: una crea el cobro y la otra recibe 409.
    // Mercado Pago se llama una sola vez y queda un solo pago.
    @Test
    void twoSimultaneousRequestsCreateOnlyOneCheckout() throws Exception {
        mercadoPagoAnswers(1000);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            start.await();
            return pay(orderCode, "CARD").andReturn().getResponse().getStatus();
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
        mercadoPago.verify(1, postRequestedFor(urlEqualTo("/v1/orders")));
        assertThat(paymentCount()).isEqualTo(1);
    }

    // El último guardia es la base: si dos peticiones pasan la revisión a la vez, el índice único
    // deja entrar solo un pago pendiente, y la segunda recibe "ya hay un pago en curso", no un error interno.
    @Test
    void theDatabaseAllowsOnlyOnePendingPaymentPerOrder() {
        Payment attempt = new Payment(null, orderId(), "mercadopago", PaymentMethod.CARD, PaymentStatus.PENDING,
                new BigDecimal("145000"), null, null);
        payments.save(attempt);

        assertThatThrownBy(() -> payments.save(attempt)).isInstanceOf(PaymentAlreadyInProgressException.class);
        assertThat(paymentCount()).isEqualTo(1);
    }

    private static void waitUntil(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.call()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("La condición no se cumplió en 5 segundos");
            }
            Thread.sleep(20);
        }
    }
}
