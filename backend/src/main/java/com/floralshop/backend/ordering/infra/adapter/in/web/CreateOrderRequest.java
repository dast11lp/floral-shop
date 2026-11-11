package com.floralshop.backend.ordering.infra.adapter.in.web;

import java.util.List;

import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.NewOrder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lo que manda el cliente para crear un pedido. Los tamaños coinciden con las columnas de la tabla.
// Los topes de líneas y unidades son de cordura; los máximos del negocio los aplica el caso de uso.
public record CreateOrderRequest(
        @NotNull @Valid Customer customer,
        @NotBlank @Size(max = 300) String deliveryAddress,
        @NotNull Long zoneId,
        @NotNull Long slotId,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Line> lines) {

    public record Customer(@NotBlank @Size(max = 120) String name,
                           @NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(max = 30) String phone) {
    }

    public record Line(@NotNull Long variantId, @NotNull @Min(1) @Max(100) Integer quantity) {
    }

    NewOrder toNewOrder() {
        return new NewOrder(new CustomerContact(customer.name().strip(), customer.email().strip(), customer.phone().strip()),
                deliveryAddress.strip(), zoneId, slotId,
                lines.stream().map(line -> new CartLine(line.variantId(), line.quantity())).toList());
    }
}