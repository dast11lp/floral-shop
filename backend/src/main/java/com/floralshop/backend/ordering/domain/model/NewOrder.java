package com.floralshop.backend.ordering.domain.model;

import java.util.List;
import java.util.Objects;

import com.floralshop.backend.catalog.domain.model.CartLine;

// Lo que pide el cliente para crear un pedido: quién es, a dónde va, en qué franja y qué compra.
public record NewOrder(CustomerContact customer, String deliveryAddress, long zoneId, long slotId,
                       List<CartLine> lines) {

    public NewOrder {
        Objects.requireNonNull(customer, "El cliente es obligatorio");
        Objects.requireNonNull(deliveryAddress, "La dirección es obligatoria");
        lines = List.copyOf(lines);
    }
}