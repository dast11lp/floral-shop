package com.floralshop.backend.payment.infra.adapter.in.web;

import com.floralshop.backend.payment.domain.model.PaymentOption;

// Una opción de pago para la tienda. El frontend decide cómo nombrarla ("Tarjeta con Mercado Pago")
// y muestra en grande la sugerida.
public record PaymentOptionResponse(String provider, String method, boolean suggested) {

    static PaymentOptionResponse from(PaymentOption option) {
        return new PaymentOptionResponse(option.provider(), option.method().name(), option.suggested());
    }
}
