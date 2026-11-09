package com.floralshop.backend.catalog.infra.adapter.in.web;

import com.floralshop.backend.catalog.domain.model.Category;

// Lo que ve el cliente de una categoría. No expone id, active ni sortOrder.
public record CategoryResponse(String name, String slug) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.name(), category.slug());
    }
}