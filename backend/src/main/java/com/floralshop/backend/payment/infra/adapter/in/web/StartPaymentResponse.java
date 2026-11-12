package com.floralshop.backend.payment.infra.adapter.in.web;

import java.math.BigDecimal;

import com.floralshop.backend.payment.domain.model.Payment;

// Lo que necesita el frontend para mandar al cliente a pagar. Sin el id interno del pago.
public record StartPaymentResponse(String status, String provider, String method, BigDecimal amount,
                                   String checkoutUrl) {

    static StartPaymentResponse from(Payment payment) {
        return new StartPaymentResponse(payment.status().name(), payment.provider(), payment.method().name(),
                payment.amount(), payment.checkoutUrl());
    }
}
