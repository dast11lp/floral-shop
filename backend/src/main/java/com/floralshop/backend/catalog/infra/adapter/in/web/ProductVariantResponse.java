package com.floralshop.backend.catalog.infra.adapter.in.web;

import java.math.BigDecimal;

import com.floralshop.backend.catalog.domain.model.ProductVariant;

// Lo que ve el cliente de una variante. El id es necesario para poder pedirla (fase 3).
public record ProductVariantResponse(Long id, String name, BigDecimal price) {

    static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(variant.id(), variant.name(), variant.price());
    }
}