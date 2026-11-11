package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;
import com.floralshop.backend.catalog.domain.model.VariantWorkload;

// Adaptador de salida: implementa el puerto del catálogo con JPA.
// Cada método es una transacción de solo lectura, para que las colecciones perezosas
// se puedan cargar mientras el mapper convierte los resultados.
@Repository
class CatalogPersistenceAdapter implements CatalogRepositoryPort {

    // Por nombre y, si dos empatan, por id: sin desempate, un producto podría repetirse entre páginas.
    private static final Sort PRODUCT_ORDER = Sort.by("name").and(Sort.by("id"));

    private final ProductJpaRepository products;
    private final CategoryJpaRepository categories;
    private final CatalogPersistenceMapper mapper = new CatalogPersistenceMapper();

    CatalogPersistenceAdapter(ProductJpaRepository products, CategoryJpaRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Product> findActiveProducts(PageQuery page) {
        return toResult(products.findByActiveTrue(toPageable(page)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Product> findActiveProductsByCategory(String categorySlug, PageQuery page) {
        return toResult(products.findActiveByCategorySlug(categorySlug, toPageable(page)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findActiveBySlug(String slug) {
        return products.findBySlugAndActiveTrue(slug).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categories.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantWorkload> findAvailableWorkloads(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of(); // "IN ()" vacío no es SQL válido
        }
        return products.findAvailableWorkloads(variantIds).stream()
                .map(row -> new VariantWorkload(row.getVariantId(), row.getVariantPreparationHours(),
                        row.getProductPreparationHours(), row.getSlotUnits()))
                .toList();
    }

    // Nuestro PageQuery -> el Pageable de Spring Data.
    private Pageable toPageable(PageQuery page) {
        return PageRequest.of(page.page(), page.size(), PRODUCT_ORDER);
    }

    // La Page de Spring Data -> nuestro PagedResult. Se llama dentro de la transacción del método.
    private PagedResult<Product> toResult(Page<ProductJpaEntity> page) {
        return new PagedResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
}