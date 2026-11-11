package com.floralshop.backend.ordering.infra.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.ordering.application.service.CreateOrderService;

import jakarta.validation.Valid;

// Adaptador de entrada: crear un pedido como invitado. No decide nada.
@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final CreateOrderService createOrder;

    public OrderController(CreateOrderService createOrder) {
        this.createOrder = createOrder;
    }

    // 201: el pedido quedó creado, pendiente de pago, con su cupo apartado.
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(createOrder.create(request.toNewOrder()));
    }
}