package com.floralshop.backend.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.ordering.application.service.OrderQueryService;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.exception.PaymentGatewayException;
import com.floralshop.backend.payment.domain.exception.PaymentOptionNotAvailableException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;

// Iniciar un pago: solo de pedidos pendientes, con una opción disponible, sin dos pagos a la vez,
// y en tres pasos (guardar el intento, llamar a la pasarela, anotar el cobro o marcar el intento fallido).
@ExtendWith(MockitoExtension.class)
class StartPaymentServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-12T15:00:00Z");
    private static final UUID CODE = UUID.fromString("7f2c0000-0000-4000-8000-000000000001");
    private static final PaymentMethodPolicy CARD =
            new PaymentMethodPolicy("mercadopago", PaymentMethod.CARD, true, 36, 1);

    @Mock OrderQueryService orders;
    @Mock PaymentMethodPolicyRepositoryPort policies;
    @Mock PaymentRepositoryPort payments;
    @Mock PaymentAttemptService attempts;
    @Mock PaymentGateway mercadoPago;

    private StartPaymentService service;

    @BeforeEach
    void setUp() {
        when(mercadoPago.provider()).thenReturn("mercadopago");
        service = new StartPaymentService(orders, policies, new PaymentGatewayRegistry(List.of(mercadoPago)),
                payments, attempts);
    }

    // Un pedido de 130.000 más 15.000 de envío: 145.000.
    private static Order order(OrderStatus status) {
        return new Order(42L, CODE, status, new CustomerContact("Ana", "ana@example.com", "3000000000"),
                "Calle 1 # 2-3", 1L, "Chapinero", new BigDecimal("15000"), 9L, LocalDate.of(2026, 11, 20),
                LocalTime.of(9, 0), LocalTime.of(11, 0), 24, 2,
                List.of(new OrderLine(5L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 1, 24, 2)),
                CREATED.plusSeconds(15 * 60), CREATED.plusSeconds(51 * 60));
    }

    // Un intento recién guardado: pendiente, todavía sin cobro en la pasarela.
    private static Payment attempt(PaymentMethod method) {
        return new Payment(7L, 42L, "mercadopago", method, PaymentStatus.PENDING, new BigDecimal("145000"),
                null, null);
    }

    private static Payment withCheckout(PaymentMethod method) {
        return attempt(method).withCheckout("ORDTST01OLD", "https://mp/old");
    }

    private void payableOrderWithCardAvailable() {
        when(orders.getByPublicCode(CODE)).thenReturn(order(OrderStatus.PENDING_PAYMENT));
        when(policies.findEnabled("mercadopago", PaymentMethod.CARD)).thenReturn(Optional.of(CARD));
        when(mercadoPago.isConfigured()).thenReturn(true);
    }

    @Test
    void savesTheAttemptThenCreatesTheCheckoutThenAttachesIt() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        Payment saved = attempt(PaymentMethod.CARD);
        when(attempts.reserve(order(OrderStatus.PENDING_PAYMENT), CARD)).thenReturn(saved);
        CheckoutSession session = new CheckoutSession("ORDTST01ABC", "https://mp/pay");
        when(mercadoPago.createCheckout(any())).thenReturn(session);
        Payment attached = saved.withCheckout("ORDTST01ABC", "https://mp/pay");
        when(attempts.attachCheckout(saved, session)).thenReturn(attached);

        assertThat(service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(attached);

        // El orden importa: el intento se guarda antes de llamar a la pasarela.
        InOrder steps = inOrder(attempts, mercadoPago);
        steps.verify(attempts).reserve(any(), any());
        steps.verify(mercadoPago).createCheckout(any());
        steps.verify(attempts).attachCheckout(saved, session);
        verify(attempts, never()).markFailed(any());
    }

    @Test
    void sendsTheOrderTotalAndAnIdempotencyKeyMadeOfOrderCodeAndAttemptId() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        when(attempts.reserve(any(), any())).thenReturn(attempt(PaymentMethod.CARD));
        when(mercadoPago.createCheckout(any())).thenReturn(new CheckoutSession("ORDTST01ABC", "https://mp/pay"));

        service.start(CODE, "mercadopago", PaymentMethod.CARD);

        ArgumentCaptor<CheckoutRequest> sent = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(mercadoPago).createCheckout(sent.capture());
        assertThat(sent.getValue().reference()).isEqualTo(CODE.toString());
        assertThat(sent.getValue().amount()).isEqualByComparingTo("145000");
        assertThat(sent.getValue().method()).isEqualTo(PaymentMethod.CARD);
        assertThat(sent.getValue().idempotencyKey()).isEqualTo(CODE + "-7");
    }

    // Si la pasarela falla, el intento queda fallido (no pendiente a medias) y el error sigue su camino.
    @Test
    void aGatewayFailureMarksTheAttemptFailed() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        Payment saved = attempt(PaymentMethod.CARD);
        when(attempts.reserve(any(), any())).thenReturn(saved);
        PaymentGatewayException failure = new PaymentGatewayException("Mercado Pago no respondió", null);
        when(mercadoPago.createCheckout(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(failure);
        verify(attempts).markFailed(saved);
        verify(attempts, never()).attachCheckout(any(), any());
    }

    // No solo los errores de la pasarela: cualquier falla al crear el cobro deja el intento fallido.
    @Test
    void anyOtherFailureWhileCreatingTheCheckoutAlsoMarksTheAttemptFailed() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        Payment saved = attempt(PaymentMethod.CARD);
        when(attempts.reserve(any(), any())).thenReturn(saved);
        when(mercadoPago.createCheckout(any())).thenThrow(new ArithmeticException("Monto con centavos"));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(ArithmeticException.class);
        verify(attempts).markFailed(saved);
    }

    @Test
    void anOrderThatIsNotPendingCannotBePaid() {
        when(orders.getByPublicCode(CODE)).thenReturn(order(OrderStatus.CONFIRMED));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(OrderNotPayableException.class);
        verifyNoInteractions(attempts);
        verify(mercadoPago, never()).createCheckout(any());
    }

    @Test
    void aDisabledOptionIsNotAvailable() {
        when(orders.getByPublicCode(CODE)).thenReturn(order(OrderStatus.PENDING_PAYMENT));
        when(policies.findEnabled("mercadopago", PaymentMethod.PSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.PSE))
                .isInstanceOf(PaymentOptionNotAvailableException.class);
        verifyNoInteractions(attempts);
    }

    @Test
    void anOptionWhoseGatewayHasNoCredentialsIsNotAvailable() {
        when(orders.getByPublicCode(CODE)).thenReturn(order(OrderStatus.PENDING_PAYMENT));
        when(policies.findEnabled("mercadopago", PaymentMethod.CARD)).thenReturn(Optional.of(CARD));
        when(mercadoPago.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(PaymentOptionNotAvailableException.class);
        verifyNoInteractions(attempts);
    }

    // El cliente recargó la página o pulsó dos veces: recibe el mismo cobro, sin crear otro.
    @Test
    void aPendingPaymentWithTheSameOptionAndItsCheckoutIsReturnedAgain() {
        payableOrderWithCardAvailable();
        Payment existing = withCheckout(PaymentMethod.CARD);
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(existing));

        assertThat(service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(existing);
        verifyNoInteractions(attempts);
        verify(mercadoPago, never()).createCheckout(any());
    }

    // Cambiar de opción con un pago en curso espera al PR 2; mientras, un conflicto con un mensaje claro.
    @Test
    void aPendingPaymentWithAnotherOptionIsAConflictWithAClearMessage() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(withCheckout(PaymentMethod.PSE)));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(PaymentAlreadyInProgressException.class)
                .hasMessageContaining("ya tiene un pago en curso");
        verifyNoInteractions(attempts);
        verify(mercadoPago, never()).createCheckout(any());
    }

    // Otra petición guardó el intento y todavía está creando el cobro: no se crea un segundo.
    @Test
    void aPendingPaymentWhoseCheckoutIsStillBeingCreatedIsAConflict() {
        payableOrderWithCardAvailable();
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(attempt(PaymentMethod.CARD)));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(PaymentAlreadyInProgressException.class);
        verifyNoInteractions(attempts);
        verify(mercadoPago, never()).createCheckout(any());
    }
}
