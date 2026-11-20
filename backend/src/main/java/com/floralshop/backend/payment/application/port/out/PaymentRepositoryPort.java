package com.floralshop.backend.payment.application.port.out;

import java.time.Instant;
import java.util.Optional;

import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Puerto de salida: guardar y buscar pagos.
public interface PaymentRepositoryPort {

    // Guarda un pago nuevo y devuelve la versión guardada (con id).
    Payment save(Payment payment);

    // El pago pendiente del pedido, si lo hay. Nunca hay más de uno (índice único en la tabla).
    Optional<Payment> findPendingByOrderId(long orderId);

    // Cuántos intentos del pedido llegaron a crear el cobro en la pasarela (los que tienen referencia),
    // en cualquier estado. Es lo que cuenta para el tope de intentos (ADR 0016, A.9).
    int countAttemptsWithCheckout(long orderId);

    // Anota el cobro creado en la pasarela, solo si el pago sigue PENDING (ADR 0016, D.8).
    // Cambia únicamente la referencia y el enlace. Devuelve false si otro proceso ya lo cerró: entonces no toca nada.
    boolean attachCheckoutIfPending(long paymentId, String providerReference, String checkoutUrl);

    // Cierra el pago como FAILED con su motivo y la hora del cierre, solo si sigue PENDING (ADR 0016, D.8).
    // Devuelve false si otro proceso ya lo cerró: entonces no toca nada.
    boolean markFailedIfPending(long paymentId, PaymentStatusReason reason, Instant resolvedAt);

    // Cierra un intento "a medio crear" (ADR 0016, D.3): FAILED, "creación sin confirmar", solo si sigue PENDING,
    // sin referencia y creado antes de createdBefore (el plazo de gracia ya pasó). Devuelve false si no cumple:
    // por ejemplo, todavía está dentro del plazo, o su enlace acaba de guardarse.
    boolean closeHalfCreatedIfOlderThan(long paymentId, Instant createdBefore, Instant resolvedAt);
}
