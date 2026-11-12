package com.floralshop.backend.payment.application.port.out;

// El cobro ya creado en la pasarela: su id allá y la URL adonde se manda al cliente.
public record CheckoutSession(String providerReference, String checkoutUrl) {
}
