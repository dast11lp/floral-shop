package com.floralshop.backend.payment.infra.adapter.in.web;

import com.floralshop.backend.payment.domain.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// La opción que eligió el cliente, tal como la devolvió GET /api/v1/payment-options.
public record StartPaymentRequest(@NotBlank @Size(max = 30) String provider, @NotNull PaymentMethod method) {
}
