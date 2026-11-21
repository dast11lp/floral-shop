package com.floralshop.backend.payment.application.port.out;

import java.time.Instant;

// El cobro ya creado en la pasarela: su id allá, la URL adonde se manda al cliente y hasta cuándo se puede pagar
// (ADR 0016, F: checkout_expires_at). expiresAt lo calcula cada adaptador, porque el vencimiento es de su pasarela.
public record CheckoutSession(String providerReference, String checkoutUrl, Instant expiresAt) {
}
