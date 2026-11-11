package com.floralshop.backend.scheduling.infra.adapter.in.web;

import java.util.List;

import com.floralshop.backend.catalog.domain.model.CartLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// El carrito y la zona que manda el cliente para ver qué franjas le sirven.
// Los topes (50 líneas, 100 unidades por línea) son de cordura, para un endpoint público.
// Los máximos del negocio (por línea y por pedido, en order_settings) los aplica el pedido.
public record DeliveryOptionsRequest(
        @NotNull Long zoneId,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Line> lines) {

    public record Line(@NotNull Long variantId, @NotNull @Min(1) @Max(100) Integer quantity) {
    }

    List<CartLine> toCartLines() {
        return lines.stream().map(line -> new CartLine(line.variantId(), line.quantity())).toList();
    }
}