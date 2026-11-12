package com.floralshop.backend.payment.infra.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.payment.application.service.PaymentOptionsService;
import com.floralshop.backend.payment.application.service.StartPaymentService;

import jakarta.validation.Valid;

// Adaptador de entrada: traduce HTTP a llamadas al servicio. No decide nada.
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentOptionsService paymentOptions;
    private final StartPaymentService startPayment;

    public PaymentController(PaymentOptionsService paymentOptions, StartPaymentService startPayment) {
        this.paymentOptions = paymentOptions;
        this.startPayment = startPayment;
    }

    // Las combinaciones de pasarela y medio que el cliente puede elegir, la sugerida primero.
    @GetMapping("/payment-options")
    public List<PaymentOptionResponse> listOptions() {
        return paymentOptions.listOptions().stream().map(PaymentOptionResponse::from).toList();
    }

    // Inicia el pago del pedido con la opción elegida y devuelve adónde mandar al cliente.
    // Si el pedido ya tiene un pago en curso con la misma opción, devuelve ese mismo.
    @PostMapping("/orders/{orderCode}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public StartPaymentResponse start(@PathVariable UUID orderCode, @Valid @RequestBody StartPaymentRequest request) {
        return StartPaymentResponse.from(startPayment.start(orderCode, request.provider(), request.method()));
    }
}
