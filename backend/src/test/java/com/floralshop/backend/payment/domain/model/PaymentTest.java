package com.floralshop.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.floralshop.backend.payment.domain.exception.InvalidPaymentTransitionException;

// Consistencia del pago y sus cambios de estado. Java puro.
class PaymentTest {

    private Payment payment(PaymentStatus status, String amount) {
        return new Payment(1L, 10L, "mercadopago", PaymentMethod.CARD, status, new BigDecimal(amount), "mp-123", null);
    }

    private Payment pending() {
        return payment(PaymentStatus.PENDING, "85000");
    }

    @Test
    void acceptsNullIdAndProviderReference() {
        // Un pago nuevo no tiene id, y un manual no tiene referencia del proveedor
        Payment manual = new Payment(null, 10L, "manual", PaymentMethod.MANUAL_TRANSFER,
                PaymentStatus.PENDING, new BigDecimal("85000"), null, null);

        assertThat(manual.id()).isNull();
        assertThat(manual.providerReference()).isNull();
    }

    @Test
    void rejectsMissingOrder() {
        assertThatThrownBy(() -> new Payment(1L, null, "mercadopago", PaymentMethod.CARD,
                PaymentStatus.PENDING, BigDecimal.TEN, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMissingMethod() {
        assertThatThrownBy(() -> new Payment(1L, 10L, "mercadopago", null,
                PaymentStatus.PENDING, BigDecimal.TEN, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMissingStatus() {
        assertThatThrownBy(() -> payment(null, "85000"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMissingAmount() {
        assertThatThrownBy(() -> new Payment(1L, 10L, "mercadopago", PaymentMethod.CARD,
                PaymentStatus.PENDING, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void rejectsBlankProvider(String provider) {
        assertThatThrownBy(() -> new Payment(1L, 10L, provider, PaymentMethod.CARD,
                PaymentStatus.PENDING, BigDecimal.TEN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1"})
    void rejectsAmountNotGreaterThanZero(String amount) {
        assertThatThrownBy(() -> payment(PaymentStatus.PENDING, amount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void transitionReturnsNewPaymentWithSameData(PaymentStatus next) {
        Payment original = pending();

        Payment moved = original.transitionTo(next);

        assertThat(moved.status()).isEqualTo(next);
        assertThat(moved).usingRecursiveComparison().ignoringFields("status").isEqualTo(original);
        assertThat(original.status()).isEqualTo(PaymentStatus.PENDING); // el original no cambia
    }

    @Test
    void pendingToPendingIsRejected() {
        assertThatThrownBy(() -> pending().transitionTo(PaymentStatus.PENDING))
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }

    // Confirmar dos veces es un error para el modelo; el caso de uso decide si lo trata como "ya pagado".
    @Test
    void paidCannotBePaidAgain() {
        Payment paid = pending().transitionTo(PaymentStatus.PAID);

        assertThatThrownBy(() -> paid.transitionTo(PaymentStatus.PAID))
                .isInstanceOf(InvalidPaymentTransitionException.class)
                .hasMessageContaining("PAID");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void finalStatesCannotGoBackToPending(PaymentStatus finalStatus) {
        Payment closed = payment(finalStatus, "85000");

        assertThatThrownBy(() -> closed.transitionTo(PaymentStatus.PENDING))
                .isInstanceOf(InvalidPaymentTransitionException.class);
    }

    @Test
    void transitionToNullIsRejected() {
        assertThatThrownBy(() -> pending().transitionTo(null))
                .isInstanceOf(NullPointerException.class);
    }

    // Un intento recién guardado no tiene cobro; al anotarlo conserva todo lo demás.
    @Test
    void withCheckoutKeepsEverythingElse() {
        Payment attempt = new Payment(7L, 10L, "mercadopago", PaymentMethod.CARD, PaymentStatus.PENDING,
                new BigDecimal("85000"), null, null);

        Payment attached = attempt.withCheckout("ORDTST01ABC", "https://mp/pay");

        assertThat(attempt.hasCheckout()).isFalse();
        assertThat(attached.hasCheckout()).isTrue();
        assertThat(attached.providerReference()).isEqualTo("ORDTST01ABC");
        assertThat(attached).usingRecursiveComparison().ignoringFields("providerReference", "checkoutUrl")
                .isEqualTo(attempt);
    }

    @Test
    void withCheckoutRequiresReferenceAndUrl() {
        assertThatThrownBy(() -> pending().withCheckout(null, "https://mp/pay"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> pending().withCheckout("ORDTST01ABC", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void isForComparesProviderAndMethod() {
        assertThat(pending().isFor("mercadopago", PaymentMethod.CARD)).isTrue();
        assertThat(pending().isFor("mercadopago", PaymentMethod.PSE)).isFalse();
        assertThat(pending().isFor("wompi", PaymentMethod.CARD)).isFalse();
    }
}
