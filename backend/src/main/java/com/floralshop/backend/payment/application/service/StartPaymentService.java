package com.floralshop.backend.payment.application.service;

import java.util.Optional;
import java.util.UUID;

import com.floralshop.backend.ordering.application.service.OrderQueryService;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.payment.application.port.out.CheckoutRequest;
import com.floralshop.backend.payment.application.port.out.CheckoutSession;
import com.floralshop.backend.payment.application.port.out.PaymentMethodPolicyRepositoryPort;
import com.floralshop.backend.payment.application.port.out.PaymentRepositoryPort;
import com.floralshop.backend.payment.domain.exception.OrderNotPayableException;
import com.floralshop.backend.payment.domain.exception.PaymentAlreadyInProgressException;
import com.floralshop.backend.payment.domain.exception.PaymentOptionNotAvailableException;
import com.floralshop.backend.payment.domain.model.Payment;
import com.floralshop.backend.payment.domain.model.PaymentMethod;
import com.floralshop.backend.payment.domain.model.PaymentMethodPolicy;

// Iniciar el pago de un pedido. Coordina tres pasos y no abre una transacción propia:
//   1. guardar el intento y extender la franja del pedido (transacción corta);
//   2. crear el cobro en la pasarela (fuera de toda transacción, ADR 0014);
//   3. anotar el cobro, o marcar el intento fallido si la pasarela falló (transacción corta).
// Java puro, sin anotaciones de Spring.
public class StartPaymentService {

    private final OrderQueryService orders;
    private final PaymentMethodPolicyRepositoryPort policies;
    private final PaymentGatewayRegistry gateways;
    private final PaymentRepositoryPort payments;
    private final PaymentAttemptService attempts;

    public StartPaymentService(OrderQueryService orders, PaymentMethodPolicyRepositoryPort policies,
                               PaymentGatewayRegistry gateways, PaymentRepositoryPort payments,
                               PaymentAttemptService attempts) {
        this.orders = orders;
        this.policies = policies;
        this.gateways = gateways;
        this.payments = payments;
        this.attempts = attempts;
    }

    public Payment start(UUID orderCode, String provider, PaymentMethod method) {
        // Solo se paga un pedido pendiente de pago.
        Order order = orders.getByPublicCode(orderCode);
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotPayableException(orderCode);
        }

        // La opción tiene que estar habilitada y su pasarela tener credenciales (ADR 0014).
        PaymentMethodPolicy policy = policies.findEnabled(provider, method)
                .filter(p -> gateways.isAvailable(p.provider()))
                .orElseThrow(() -> new PaymentOptionNotAvailableException(provider, method));

        // Si ya hay un pago pendiente con la misma opción y su cobro ya existe (el cliente recargó la página
        // o pulsó dos veces), se devuelve ese mismo. Si es otra opción, o el cobro todavía se está creando,
        // el cliente espera: cambiar de opción de forma segura necesita consultar y cancelar el cobro en la
        // pasarela, y eso llega en el PR 2. La espera está acotada por el tope de la franja del pedido.
        Optional<Payment> pending = payments.findPendingByOrderId(order.id());
        if (pending.isPresent()) {
            if (pending.get().isFor(provider, method) && pending.get().hasCheckout()) {
                return pending.get();
            }
            throw new PaymentAlreadyInProgressException();
        }

        // Paso 1.
        Payment attempt = attempts.reserve(order, policy);

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
            attempts.markFailed(attempt);
            throw ex;
        }

        // Paso 3.
        return attempts.attachCheckout(attempt, session);
    }
}
