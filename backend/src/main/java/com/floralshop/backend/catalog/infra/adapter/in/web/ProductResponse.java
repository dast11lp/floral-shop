package com.floralshop.backend.catalog.infra.adapter.in.web;

import java.util.List;

import com.floralshop.backend.catalog.domain.model.Product;

// Lo que ve el cliente de un producto. El slug es su identificador público.
public record ProductResponse(String name, String slug, String description,
                              List<ProductVariantResponse> variants,
                              List<CategoryResponse> categories) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.name(),
                product.slug(),
                product.description(),
                product.variants().stream().map(ProductVariantResponse::from).toList(),
                product.categories().stream().map(CategoryResponse::from).toList());
    }
}