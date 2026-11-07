package com.floralshop.backend.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;

// Puerto de salida del catálogo: solo lectura. Lo implementa un adapter de persistencia en infra.
// Todos los métodos "Active" devuelven únicamente lo que se puede mostrar al público:
// productos activos, con sus variantes activas y sus categorías activas.
public interface CatalogRepositoryPort {

    PagedResult<Product> findActiveProducts(PageQuery page);

    PagedResult<Product> findActiveProductsByCategory(String categorySlug, PageQuery page);

    Optional<Product> findActiveBySlug(String slug);

    List<Category> findActiveCategories();
}