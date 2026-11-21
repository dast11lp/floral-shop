package com.floralshop.backend.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.application.service.OrderLockService;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentSettingsRepositoryPort;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.exception.PaymentAttemptClosedException;
import com.floralshop.backend.payment.domain.exception.PaymentAttemptsExhaustedException;
import com.floralshop.backend.payment.domain.exception.PaymentStartingException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Los pasos de un intento de pago que tocan la base: guardar el intento (con el pedido bajo candado y sus
// revisiones), anotar el cobro y marcarlo fallido. Anotar y marcar fallido son actualizaciones condicionales
// (ADR 0016, D.8): solo si el intento sigue pendiente.
@ExtendWith(MockitoExtension.class)
class PaymentAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T15:05:00Z");
    private static final UUID CODE = UUID.fromString("7f2c0000-0000-4000-8000-000000000001");
    private static final Duration GRACE = Duration.ofMinutes(1);
    private static final Instant EXPIRES = NOW.plusSeconds(15 * 60);
    private static final PaymentMethodPolicy CARD =
            new PaymentMethodPolicy("mercadopago", PaymentMethod.CARD, true, 36, 1);

    @Mock PaymentRepositoryPort payments;
    @Mock PaymentSettingsRepositoryPort settings;
    @Mock OrderLockService orderLocks;
    @Mock OrderHoldService holds;

    private PaymentAttemptService service;

    @BeforeEach
    void setUp() {
        service = new PaymentAttemptService(payments, settings, orderLocks, holds, Clock.fixed(NOW, ZoneOffset.UTC),
                GRACE);
    }

    // Creado hace 5 minutos; su retención vence a los 15 (dentro de 10 minutos) y su tope es a los 51.
    private static Order order(OrderStatus status, Instant holdExpiresAt) {
        Instant created = NOW.minusSeconds(5 * 60);
        return new Order(42L, CODE, status,
                new CustomerContact("Ana", "ana@example.com", "3000000000"), "Calle 1 # 2-3", 1L, "Chapinero",
                new BigDecimal("15000"), 9L, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                24, 2, List.of(new OrderLine(5L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 1, 24, 2)),
                holdExpiresAt, created.plusSeconds(51 * 60));
    }

    private static Order payableOrder() {
        return order(OrderStatus.PENDING_PAYMENT, NOW.plusSeconds(10 * 60));
    }

    private static Payment attempt(PaymentMethod method) {
        return new Payment(7L, 42L, "mercadopago", method, PaymentStatus.PENDING,
                new BigDecimal("145000"), null, null);
    }

    private static Payment attempt() {
        return attempt(PaymentMethod.CARD);
    }

    // --- reserve: el caso normal ---

    @Test
    void reserveLocksTheOrderSavesAPendingAttemptAndExtendsTheHoldTheMinutesOfTheOption() {
        Order order = payableOrder();
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(order);
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        when(settings.maxAttemptsPerOrder()).thenReturn(5);
        when(payments.countAttemptsWithCheckout(42L)).thenReturn(4);
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));

        PaymentReservation reservation = service.reserve(CODE, CARD);

        ArgumentCaptor<Payment> sent = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(sent.capture());
        assertThat(sent.getValue().id()).isNull();
        assertThat(sent.getValue().orderId()).isEqualTo(42L);
        assertThat(sent.getValue().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(sent.getValue().amount()).isEqualByComparingTo("145000");
        assertThat(sent.getValue().hasCheckout()).isFalse();
        assertThat(reservation.payment()).isSameAs(sent.getValue());
        assertThat(reservation.order()).isSameAs(order);
        assertThat(reservation.reused()).isFalse();
        // 36 minutos desde ahora; el tope lo aplica OrderHoldService.
        verify(holds).extendHold(order, NOW.plusSeconds(36 * 60));
    }

    // --- reserve: el pedido ---

    @Test
    void anOrderThatIsNotPendingCannotBePaid() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(order(OrderStatus.CANCELLED, NOW.plusSeconds(600)));

        assertThatThrownBy(() -> service.reserve(CODE, CARD)).isInstanceOf(OrderNotPayableException.class);
        verify(payments, never()).save(any());
        verifyNoInteractions(holds);
    }

    // La retención ya venció aunque el job todavía no haya pasado: no se crea un intento (A.9).
    @Test
    void anOrderWhoseHoldAlreadyExpiredCannotBePaid() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(order(OrderStatus.PENDING_PAYMENT, NOW));

        assertThatThrownBy(() -> service.reserve(CODE, CARD)).isInstanceOf(OrderNotPayableException.class);
        verify(payments, never()).save(any());
        verifyNoInteractions(holds);
    }

    // --- reserve: el pago pendiente que ya tenía el pedido ---

    // El cliente recargó la página o pulsó dos veces: recibe el mismo cobro, sin crear otro.
    @Test
    void aPendingPaymentWithTheSameOptionAndItsCheckoutIsReused() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(payableOrder());
        Payment existing = attempt().withCheckout("ORDTST01OLD", "https://mp/old");
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(existing));

        PaymentReservation reservation = service.reserve(CODE, CARD);

        assertThat(reservation.reused()).isTrue();
        assertThat(reservation.payment()).isSameAs(existing);
        verify(payments, never()).save(any());
        verifyNoInteractions(holds);
    }

    // Cambiar de opción con un pago en curso llega en el PR 2b; mientras, un conflicto con un mensaje claro.
    @Test
    void aPendingPaymentWithAnotherOptionIsAConflict() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(payableOrder());
        when(payments.findPendingByOrderId(42L))
                .thenReturn(Optional.of(attempt(PaymentMethod.PSE).withCheckout("ORDTST01OLD", "https://mp/old")));

        assertThatThrownBy(() -> service.reserve(CODE, CARD))
                .isInstanceOf(PaymentAlreadyInProgressException.class)
                .hasMessageContaining("ya tiene un pago en curso");
        verify(payments, never()).save(any());
    }

    // Otra petición guardó el intento hace menos de un minuto y todavía está creando el cobro (D.3).
    @Test
    void anAttemptWithoutCheckoutInsideTheGracePeriodMeansThePaymentIsStarting() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(payableOrder());
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(attempt()));
        when(payments.closeHalfCreatedIfOlderThan(7L, NOW.minus(GRACE), NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.reserve(CODE, CARD))
                .isInstanceOf(PaymentStartingException.class)
                .hasMessageContaining("se está iniciando");
        verify(payments, never()).save(any());
    }

    // Pasado el plazo de gracia, el intento quedó a medio crear: se cierra y se crea uno nuevo (D.3).
    @Test
    void aHalfCreatedAttemptPastTheGracePeriodIsClosedAndANewOneIsSaved() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(payableOrder());
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.of(attempt()));
        when(payments.closeHalfCreatedIfOlderThan(7L, NOW.minus(GRACE), NOW)).thenReturn(true);
        when(settings.maxAttemptsPerOrder()).thenReturn(5);
        when(payments.countAttemptsWithCheckout(42L)).thenReturn(0);
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));

        PaymentReservation reservation = service.reserve(CODE, CARD);

        assertThat(reservation.reused()).isFalse();
        verify(payments).save(any());
    }

    // --- reserve: el tope de intentos ---

    @Test
    void anOrderThatReachedTheMaximumOfAttemptsCannotTryAgain() {
        when(orderLocks.lockByPublicCode(CODE)).thenReturn(payableOrder());
        when(payments.findPendingByOrderId(42L)).thenReturn(Optional.empty());
        when(settings.maxAttemptsPerOrder()).thenReturn(5);
        when(payments.countAttemptsWithCheckout(42L)).thenReturn(5);

        assertThatThrownBy(() -> service.reserve(CODE, CARD))
                .isInstanceOf(PaymentAttemptsExhaustedException.class)
                .hasMessageContaining("máximo de intentos");
        verify(payments, never()).save(any());
        verifyNoInteractions(holds);
    }

    // --- attachCheckout y markFailed ---

    @Test
    void attachCheckoutAnnotatesTheReferenceAndTheUrlWhenTheAttemptIsStillPending() {
        when(payments.attachCheckoutIfPending(7L, "ORDTST01ABC", "https://mp/pay", EXPIRES)).thenReturn(true);

        Payment attached = service.attachCheckout(attempt(), new CheckoutSession("ORDTST01ABC", "https://mp/pay", EXPIRES));

        assertThat(attached.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attached.providerReference()).isEqualTo("ORDTST01ABC");
        assertThat(attached.checkoutUrl()).isEqualTo("https://mp/pay");
        assertThat(attached.id()).isEqualTo(7L);
        // Nunca reescribe la fila entera (ADR 0016, D.8).
        verify(payments, never()).save(any());
    }

    // Otro proceso ya cerró el intento: no se entrega el enlace, y el error no lo menciona (D.3).
    @Test
    void attachCheckoutFailsWithoutTheUrlWhenAnotherProcessAlreadyClosedTheAttempt() {
        when(payments.attachCheckoutIfPending(7L, "ORDTST01ABC", "https://mp/pay", EXPIRES)).thenReturn(false);

        assertThatThrownBy(() -> service.attachCheckout(attempt(), new CheckoutSession("ORDTST01ABC", "https://mp/pay", EXPIRES)))
                .isInstanceOf(PaymentAttemptClosedException.class)
                .message().doesNotContain("https://mp/pay");
    }

    @Test
    void markFailedClosesTheAttemptWithItsReasonAndTheTime() {
        service.markFailed(attempt(), PaymentStatusReason.CREATION_UNCONFIRMED);

        verify(payments).markFailedIfPending(7L, PaymentStatusReason.CREATION_UNCONFIRMED, NOW);
        verify(payments, never()).save(any());
    }

    // Si otro proceso ya lo cerró, no pasa nada: su cierre se respeta y no es un error.
    @Test
    void markFailedOnAnAlreadyClosedAttemptChangesNothing() {
        when(payments.markFailedIfPending(anyLong(), any(), any())).thenReturn(false);

        service.markFailed(attempt(), PaymentStatusReason.GATEWAY_REJECTED);

        verify(payments).markFailedIfPending(7L, PaymentStatusReason.GATEWAY_REJECTED, NOW);
    }
}
