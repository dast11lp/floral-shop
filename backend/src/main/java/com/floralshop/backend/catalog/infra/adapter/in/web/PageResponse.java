package com.floralshop.backend.catalog.infra.adapter.in.web;

import java.util.List;
import java.util.function.Function;

import com.floralshop.backend.catalog.domain.model.PagedResult;

// Una página de resultados tal como la ve el cliente. Las páginas empiezan en 0.
// Si otro módulo necesita paginar, este tipo se moverá a shared junto con PagedResult.
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    // Convierte cada elemento del dominio con la función que se le pase.
    static <S, T> PageResponse<T> from(PagedResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}