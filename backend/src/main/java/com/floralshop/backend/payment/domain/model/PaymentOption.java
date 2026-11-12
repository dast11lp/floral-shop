package com.floralshop.backend.payment.domain.model;

// Lo que el cliente puede elegir al pagar: una pasarela con un medio.
// suggested marca la que la tienda muestra en grande; las demás van en pequeño.
public record PaymentOption(String provider, PaymentMethod method, boolean suggested) {
}
