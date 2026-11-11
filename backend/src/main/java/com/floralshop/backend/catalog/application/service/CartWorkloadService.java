package com.floralshop.backend.catalog.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.CartWorkload;
import com.floralshop.backend.catalog.domain.model.VariantWorkload;

// Cuánto trabajo pide un carrito (ADR 0012, punto 3). Java puro, sin anotaciones de Spring.
//
// La regla vive SOLO aquí: la florista trabaja sola y arma un producto después del otro,
// así que las horas se suman, línea por línea y unidad por unidad. Los cupos se suman igual.
// Si algún día la regla cambia (que mande el más lento, sumar con descuento), se cambia este servicio.
public class CartWorkloadService {

    private final CatalogRepositoryPort catalog;

    public CartWorkloadService(CatalogRepositoryPort catalog) {
        this.catalog = catalog;
    }

    public CartWorkload estimate(List<CartLine> lines) {
        Objects.requireNonNull(lines, "Las líneas son obligatorias");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        List<Long> ids = lines.stream().map(CartLine::variantId).distinct().toList();
        Map<Long, VariantWorkload> byId = catalog.findAvailableWorkloads(ids).stream()
                .collect(Collectors.toMap(VariantWorkload::variantId, Function.identity()));

        int hours = 0;
        int units = 0;
        for (CartLine line : lines) {
            VariantWorkload variant = byId.get(line.variantId());
            if (variant == null) {
                throw new VariantNotAvailableException(line.variantId());
            }
            // multiplyExact y addExact fallan en vez de dar un número absurdo si algo se desborda
            hours = Math.addExact(hours, Math.multiplyExact(variant.craftingHours(), line.quantity()));
            units = Math.addExact(units, Math.multiplyExact(variant.slotUnits(), line.quantity()));
        }
        return new CartWorkload(hours, units);
    }
}