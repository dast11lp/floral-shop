package com.floralshop.backend.payment.application.service;

import java.util.UUID;

import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.domain.exception.PaymentOptionNotAvailableException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;
import com.floralshop.backend.payment.domain.model.PaymentStatusReason;

// Iniciar el pago de un pedido. Coordina tres pasos y no abre una transacción propia:
//   1. con el pedido bajo candado, revisar que se pueda pagar, guardar el intento y extender la franja
//      (transacción corta; las revisiones están en PaymentAttemptService.reserve);
//   2. crear el cobro en la pasarela (fuera de toda transacción, ADR 0014);
//   3. anotar el cobro, o marcar el intento fallido si la pasarela falló (transacción corta).
// Java puro, sin anotaciones de Spring.
public class StartPaymentService {

    private final PaymentMethodPolicyRepositoryPort policies;
    private final PaymentGatewayRegistry gateways;
    private final PaymentAttemptService attempts;

    public StartPaymentService(PaymentMethodPolicyRepositoryPort policies, PaymentGatewayRegistry gateways,
                               PaymentAttemptService attempts) {
        this.policies = policies;
        this.gateways = gateways;
        this.attempts = attempts;
    }

    public Payment start(UUID orderCode, String provider, PaymentMethod method) {
        // La opción tiene que estar habilitada y su pasarela tener credenciales (ADR 0014).
        // Va antes del paso 1 porque sus minutos son los que se extiende la franja.
        PaymentMethodPolicy policy = policies.findEnabled(provider, method)
                .filter(p -> gateways.isAvailable(p.provider()))
                .orElseThrow(() -> new PaymentOptionNotAvailableException(provider, method));

        // Paso 1. Si ya había un pago con su enlace y la misma opción, se devuelve ese, sin crear otro.
        PaymentReservation reservation = attempts.reserve(orderCode, policy);
        if (reservation.reused()) {
            return reservation.payment();
        }
        Order order = reservation.order();
        Payment attempt = reservation.payment();

        // Paso 2. La llave de idempotencia combina el código del pedido (aleatorio) con el id del intento:
        // es única aunque la base de datos se recree y los ids vuelvan a empezar.
        CheckoutSession session;
        try {
            session = gateways.get(provider).createCheckout(new CheckoutRequest(
                    order.publicCode().toString(), "Pedido " + order.publicCode(), order.total(), method,
                    order.publicCode() + "-" + attempt.id()));
        } catch (RuntimeException ex) {
            // Cualquier falla (la pasarela no respondió, rechazó la petición, o un monto con centavos) deja
            // el intento fallido. Si quedara pendiente sin cobro, el pedido no podría volver a intentar.
            // Por ahora todo fallo es "creación sin confirmar"; separar el rechazo de la pasarela llega en el paso 5.
            attempts.markFailed(attempt, PaymentStatusReason.CREATION_UNCONFIRMED);
            throw ex;
        }

        // Paso 3. Si otro proceso ya cerró el intento, sale PaymentAttemptClosedException, sin el enlace.
        return attempts.attachCheckout(attempt, session);
    }
}
