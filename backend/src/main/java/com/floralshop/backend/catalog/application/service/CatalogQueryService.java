package com.floralshop.backend.catalog.application.service;

import java.util.Collection;
import java.util.List;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.exception.ProductNotFoundException;
import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.OrderableVariant;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;

// Consultas del catálogo público. Java puro, sin anotaciones de Spring:
// se registra como bean en infra/config (2.5b), junto con su adapter.
public class CatalogQueryService {

    private final CatalogRepositoryPort catalog;

    public CatalogQueryService(CatalogRepositoryPort catalog) {
        this.catalog = catalog;
    }

    // Sin categoría (null o en blanco): todo el catálogo. Con categoría: solo sus productos.
    public PagedResult<Product> listProducts(String categorySlug, PageQuery page) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return catalog.findActiveProducts(page);
        }
        return catalog.findActiveProductsByCategory(categorySlug, page);
    }

    // Un producto por su slug. Si no existe o está inactivo, error de negocio.
    public Product getProduct(String slug) {
        return catalog.findActiveBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    public List<Category> listCategories() {
        return catalog.findActiveCategories();
    }

    // Las variantes pedidas que están a la venta, con lo que el pedido copia.
    // Las que no existen o no están a la venta no vienen: quien llama decide qué hacer.
    public List<OrderableVariant> orderableVariants(Collection<Long> variantIds) {
        return catalog.findOrderableVariants(variantIds);
    }
}