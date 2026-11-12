package com.floralshop.backend.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatus;

// Los pasos de un intento de pago que tocan la base: guardar el intento, anotar el cobro, marcarlo fallido.
@ExtendWith(MockitoExtension.class)
class PaymentAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T15:05:00Z");
    private static final PaymentMethodPolicy CARD =
            new PaymentMethodPolicy("mercadopago", PaymentMethod.CARD, true, 36, 1);

    @Mock PaymentRepositoryPort payments;
    @Mock OrderHoldService holds;

    private PaymentAttemptService service;

    @BeforeEach
    void setUp() {
        service = new PaymentAttemptService(payments, holds, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Order order() {
        Instant created = NOW.minusSeconds(5 * 60);
        return new Order(42L, UUID.randomUUID(), OrderStatus.PENDING_PAYMENT,
                new CustomerContact("Ana", "ana@example.com", "3000000000"), "Calle 1 # 2-3", 1L, "Chapinero",
                new BigDecimal("15000"), 9L, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                24, 2, List.of(new OrderLine(5L, "Ramo de rosas", "Grande", new BigDecimal("130000"), 1, 24, 2)),
                created.plusSeconds(15 * 60), created.plusSeconds(51 * 60));
    }

    private static Payment attempt() {
        return new Payment(7L, 42L, "mercadopago", PaymentMethod.CARD, PaymentStatus.PENDING,
                new BigDecimal("145000"), null, null);
    }

    @Test
    void reserveSavesAPendingAttemptWithoutCheckoutAndExtendsTheHoldTheMinutesOfTheOption() {
        Order order = order();
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));

        Payment saved = service.reserve(order, CARD);

        ArgumentCaptor<Payment> sent = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(sent.capture());
        assertThat(sent.getValue().id()).isNull();
        assertThat(sent.getValue().orderId()).isEqualTo(42L);
        assertThat(sent.getValue().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(sent.getValue().amount()).isEqualByComparingTo("145000");
        assertThat(sent.getValue().hasCheckout()).isFalse();
        assertThat(saved).isSameAs(sent.getValue());
        // 36 minutos desde ahora; el tope lo aplica OrderHoldService.
        verify(holds).extendHold(order, NOW.plusSeconds(36 * 60));
    }

    // Si otra petición ya tiene un pago pendiente, el guardado falla y la franja no se toca.
    @Test
    void reserveDoesNotExtendTheHoldWhenAnotherPaymentIsInProgress() {
        when(payments.save(any())).thenThrow(new PaymentAlreadyInProgressException());

        assertThatThrownBy(() -> service.reserve(order(), CARD))
                .isInstanceOf(PaymentAlreadyInProgressException.class);
        verifyNoInteractions(holds);
    }

    @Test
    void attachCheckoutSavesTheReferenceAndTheUrl() {
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));

        Payment attached = service.attachCheckout(attempt(), new CheckoutSession("ORDTST01ABC", "https://mp/pay"));

        assertThat(attached.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attached.providerReference()).isEqualTo("ORDTST01ABC");
        assertThat(attached.checkoutUrl()).isEqualTo("https://mp/pay");
        assertThat(attached.id()).isEqualTo(7L);
    }

    @Test
    void markFailedLeavesTheAttemptFailed() {
        service.markFailed(attempt());

        ArgumentCaptor<Payment> sent = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(sent.capture());
        assertThat(sent.getValue().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(sent.getValue().id()).isEqualTo(7L);
    }
}
