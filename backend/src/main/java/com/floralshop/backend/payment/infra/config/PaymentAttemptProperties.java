package com.floralshop.backend.payment.infra.config;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuración de los intentos de pago (payment.attempt.* en application.yaml).
// creationGrace: cuánto se espera a un intento sin enlace antes de darlo por "a medio crear" y cerrarlo
// (ADR 0016, D.3). Va en la configuración y no en la base porque depende de los tiempos de espera con la
// pasarela: tiene que ser mayor que la espera máxima de MercadoPagoGateway (5 s de conexión + 15 s de lectura).
// Si no, se cerraría un intento cuya respuesta todavía puede llegar.
@ConfigurationProperties("payment.attempt")
record PaymentAttemptProperties(Duration creationGrace) {

    private static final Duration LONGEST_GATEWAY_WAIT = Duration.ofSeconds(20);

    PaymentAttemptProperties {
        Objects.requireNonNull(creationGrace, "Falta payment.attempt.creation-grace");
        if (creationGrace.compareTo(LONGEST_GATEWAY_WAIT) <= 0) {
            throw new IllegalArgumentException("payment.attempt.creation-grace debe ser mayor que la espera máxima "
                    + "con la pasarela (" + LONGEST_GATEWAY_WAIT.toSeconds() + " s)");
        }
    }
}
