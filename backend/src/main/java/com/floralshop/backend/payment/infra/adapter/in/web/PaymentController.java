package com.floralshop.backend.payment.infra.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.payment.application.service.PaymentOptionsService;

// Adaptador de entrada: traduce HTTP a llamadas al servicio. No decide nada.
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentOptionsService paymentOptions;

    public PaymentController(PaymentOptionsService paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    // Las combinaciones de pasarela y medio que el cliente puede elegir, la sugerida primero.
    @GetMapping("/payment-options")
    public List<PaymentOptionResponse> listOptions() {
        return paymentOptions.listOptions().stream().map(PaymentOptionResponse::from).toList();
    }
}
