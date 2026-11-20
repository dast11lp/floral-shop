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

import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentGateway;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAttemptClosedException;
import com.floralshop.backend.payment.domain.exception.PaymentGatewayException;
import com.floralshop.backend.payment.domain.exception.PaymentOptionNotAvailableException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Iniciar un pago coordina tres pasos: guardar el intento (con sus revisiones, en PaymentAttemptService),
// llamar a la pasarela, y anotar el cobro o marcar el intento fallido. Las revisiones con el candado del pedido
// se prueban en PaymentAttemptServiceTest y, con PostgreSQL real, en StartPaymentApiIntegrationTest.
@ExtendWith(MockitoExtension.class)
class StartPaymentServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-12T15:00:00Z");
    private static final UUID CODE = UUID.fromString("7f2c0000-0000-4000-8000-000000000001");
    private static final PaymentMethodPolicy CARD =
            new PaymentMethodPolicy("mercadopago", PaymentMethod.CARD, true, 36, 1);

    @Mock PaymentMethodPolicyRepositoryPort policies;
    @Mock PaymentAttemptService attempts;
    @Mock PaymentGateway mercadoPago;

    private StartPaymentService service;

    @BeforeEach
    void setUp() {
        when(mercadoPago.provider()).thenReturn("mercadopago");
        service = new StartPaymentService(policies, new PaymentGatewayRegistry(List.of(mercadoPago)), attempts);
    }

    // Un pedido de 130.000 más 15.000 de envío: 145.000.
    private static Order order() {
        return new Order(42L, CODE, OrderStatus.PENDING_PAYMENT,
                new CustomerContact("Ana", "ana@example.com", "3000000000"), "Calle 1 # 2-3", 1L, "Chapinero",
                new BigDecimal("15000"), 9L, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                24, 2, List.of(new OrderLine(5L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 1, 24, 2)),
                CREATED.plusSeconds(15 * 60), CREATED.plusSeconds(51 * 60));
    }

    // Un intento recién guardado: pendiente, todavía sin cobro en la pasarela.
    private static Payment attempt() {
        return new Payment(7L, 42L, "mercadopago", PaymentMethod.CARD, PaymentStatus.PENDING,
                new BigDecimal("145000"), null, null);
    }

    private void cardAvailable() {
        when(policies.findEnabled("mercadopago", PaymentMethod.CARD)).thenReturn(Optional.of(CARD));
        when(mercadoPago.isConfigured()).thenReturn(true);
    }

    private Payment newAttemptReserved() {
        cardAvailable();
        Payment saved = attempt();
        when(attempts.reserve(CODE, CARD)).thenReturn(new PaymentReservation(order(), saved, false));
        return saved;
    }

    @Test
    void savesTheAttemptThenCreatesTheCheckoutThenAttachesIt() {
        Payment saved = newAttemptReserved();
        CheckoutSession session = new CheckoutSession("ORDTST01ABC", "https://mp/pay");
        when(mercadoPago.createCheckout(any())).thenReturn(session);
        Payment attached = saved.withCheckout("ORDTST01ABC", "https://mp/pay");
        when(attempts.attachCheckout(saved, session)).thenReturn(attached);

        assertThat(service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(attached);

        // El orden importa: el intento se guarda antes de llamar a la pasarela.
        InOrder steps = inOrder(attempts, mercadoPago);
        steps.verify(attempts).reserve(CODE, CARD);
        steps.verify(mercadoPago).createCheckout(any());
        steps.verify(attempts).attachCheckout(saved, session);
        verify(attempts, never()).markFailed(any(), any());
    }

    @Test
    void sendsTheOrderTotalAndAnIdempotencyKeyMadeOfOrderCodeAndAttemptId() {
        newAttemptReserved();
        when(mercadoPago.createCheckout(any())).thenReturn(new CheckoutSession("ORDTST01ABC", "https://mp/pay"));

        service.start(CODE, "mercadopago", PaymentMethod.CARD);

        ArgumentCaptor<CheckoutRequest> sent = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(mercadoPago).createCheckout(sent.capture());
        assertThat(sent.getValue().reference()).isEqualTo(CODE.toString());
        assertThat(sent.getValue().amount()).isEqualByComparingTo("145000");
        assertThat(sent.getValue().method()).isEqualTo(PaymentMethod.CARD);
        assertThat(sent.getValue().idempotencyKey()).isEqualTo(CODE + "-7");
    }

    // El cliente recargó la página o pulsó dos veces: recibe el mismo cobro, sin llamar a la pasarela.
    @Test
    void aReusedPaymentIsReturnedWithoutCallingTheGateway() {
        cardAvailable();
        Payment existing = attempt().withCheckout("ORDTST01OLD", "https://mp/old");
        when(attempts.reserve(CODE, CARD)).thenReturn(new PaymentReservation(order(), existing, true));

        assertThat(service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(existing);
        verify(mercadoPago, never()).createCheckout(any());
        verify(attempts, never()).attachCheckout(any(), any());
    }

    // Si la pasarela falla, el intento queda fallido (no pendiente a medias) y el error sigue su camino.
    @Test
    void aGatewayFailureMarksTheAttemptFailed() {
        Payment saved = newAttemptReserved();
        PaymentGatewayException failure = new PaymentGatewayException("Mercado Pago no respondió", null);
        when(mercadoPago.createCheckout(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD)).isSameAs(failure);
        verify(attempts).markFailed(saved, PaymentStatusReason.CREATION_UNCONFIRMED);
        verify(attempts, never()).attachCheckout(any(), any());
    }

    // No solo los errores de la pasarela: cualquier falla al crear el cobro deja el intento fallido.
    @Test
    void anyOtherFailureWhileCreatingTheCheckoutAlsoMarksTheAttemptFailed() {
        Payment saved = newAttemptReserved();
        when(mercadoPago.createCheckout(any())).thenThrow(new ArithmeticException("Monto con centavos"));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(ArithmeticException.class);
        verify(attempts).markFailed(saved, PaymentStatusReason.CREATION_UNCONFIRMED);
    }

    // Si otro proceso cerró el intento mientras se esperaba a la pasarela, el cliente no recibe el enlace
    // y el intento no se marca fallido: su cierre se respeta (ADR 0016, D.8).
    @Test
    void whenTheAttemptWasClosedMeanwhileTheClientGetsNoCheckout() {
        Payment saved = newAttemptReserved();
        CheckoutSession session = new CheckoutSession("ORDTST01ABC", "https://mp/pay");
        when(mercadoPago.createCheckout(any())).thenReturn(session);
        when(attempts.attachCheckout(saved, session)).thenThrow(new PaymentAttemptClosedException());

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(PaymentAttemptClosedException.class)
                .message().doesNotContain("https://mp/pay");
        verify(attempts, never()).markFailed(any(), any());
    }

    // Si la revisión con el candado rechaza el pago, no se llama a la pasarela.
    @Test
    void whenTheReservationIsRejectedTheGatewayIsNotCalled() {
        cardAvailable();
        when(attempts.reserve(CODE, CARD)).thenThrow(new OrderNotPayableException(CODE));

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(OrderNotPayableException.class);
        verify(mercadoPago, never()).createCheckout(any());
    }

    @Test
    void aDisabledOptionIsNotAvailable() {
        when(policies.findEnabled("mercadopago", PaymentMethod.PSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.PSE))
                .isInstanceOf(PaymentOptionNotAvailableException.class);
        verifyNoInteractions(attempts);
    }

    @Test
    void anOptionWhoseGatewayHasNoCredentialsIsNotAvailable() {
        when(policies.findEnabled("mercadopago", PaymentMethod.CARD)).thenReturn(Optional.of(CARD));
        when(mercadoPago.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.start(CODE, "mercadopago", PaymentMethod.CARD))
                .isInstanceOf(PaymentOptionNotAvailableException.class);
        verifyNoInteractions(attempts);
    }
}
