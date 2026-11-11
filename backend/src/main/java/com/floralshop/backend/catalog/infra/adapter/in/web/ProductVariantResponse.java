package com.floralshop.backend.catalog.infra.adapter.in.web;

import java.math.BigDecimal;

import com.floralshop.backend.catalog.domain.model.Product;
import com.floralshop.backend.catalog.domain.model.ProductVariant;

// Lo que ve el cliente de una variante. El id es necesario para poder pedirla (fase 3).
// craftingHours es el tiempo que de verdad aplica: el de la variante o, si no tiene, el del producto.
// Va en horas; el front decide si lo muestra como horas o días (ADR 0012).
// No expone los cupos que ocupa: es un dato interno de la florista, como la capacidad de las franjas.
public record ProductVariantResponse(Long id, String name, BigDecimal price, int craftingHours) {

    static ProductVariantResponse from(Product product, ProductVariant variant) {
        return new ProductVariantResponse(variant.id(), variant.name(), variant.price(),
                product.craftingHoursOf(variant));
    }
}