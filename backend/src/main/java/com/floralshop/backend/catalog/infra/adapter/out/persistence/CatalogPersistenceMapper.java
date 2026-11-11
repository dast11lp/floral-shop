package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.Product;
import com.floralshop.backend.catalog.domain.model.ProductVariant;

// Convierte las entidades de JPA en modelos de dominio. Solo en esa dirección: el catálogo es de lectura.
// Pasa por los constructores de los records, que validan la consistencia.
class CatalogPersistenceMapper {

    Category toDomain(CategoryJpaEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getSlug(),
                entity.isActive(), entity.getSortOrder());
    }

    ProductVariant toDomain(ProductVariantJpaEntity entity) {
        return new ProductVariant(entity.getId(), entity.getName(), entity.getPrice(),
                entity.isActive(), entity.getSortOrder(),
                entity.getCraftingHours(), entity.getSlotUnits());
    }

    // Recorre las colecciones perezosas (variantes y categorías): por eso debe ejecutarse
    // dentro de una transacción abierta. Lo garantiza el @Transactional del adapter.
    Product toDomain(ProductJpaEntity entity) {
        return new Product(entity.getId(), entity.getName(), entity.getSlug(),
                entity.getDescription(), entity.isActive(), entity.getCraftingHours(),
                entity.getVariants().stream().map(this::toDomain).toList(),
                entity.getCategories().stream().map(this::toDomain).toList());
    }
}