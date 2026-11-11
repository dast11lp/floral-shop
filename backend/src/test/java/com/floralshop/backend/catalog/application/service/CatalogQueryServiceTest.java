package com.floralshop.backend.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.exception.ProductNotFoundException;
import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;

// Prueba unitaria con Mockito: sin Spring ni base de datos.
@ExtendWith(MockitoExtension.class)
class CatalogQueryServiceTest {

    @Mock
    CatalogRepositoryPort catalog; // doble del puerto

    CatalogQueryService service;

    private final PageQuery page = new PageQuery(0, 10);

    @BeforeEach
    void setUp() {
        service = new CatalogQueryService(catalog);
    }

    private Product product(String slug) {
        return new Product(1L, "Ramo", slug, "Descripción", true, 24, List.of(), List.of());
    }

    private PagedResult<Product> pageOf(Product product) {
        return new PagedResult<>(List.of(product), 0, 10, 1);
    }

    @Test
    void listsWholeCatalogWhenNoCategory() {
        PagedResult<Product> expected = pageOf(product("ramo"));
        when(catalog.findActiveProducts(page)).thenReturn(expected);

        assertThat(service.listProducts(null, page)).isSameAs(expected);
        verify(catalog, never()).findActiveProductsByCategory(any(), any());
    }

    @Test
    void blankCategoryMeansNoFilter() {
        PagedResult<Product> expected = pageOf(product("ramo"));
        when(catalog.findActiveProducts(page)).thenReturn(expected);

        assertThat(service.listProducts("   ", page)).isSameAs(expected);
    }

    @Test
    void filtersByCategoryWhenGiven() {
        PagedResult<Product> expected = pageOf(product("rosa"));
        when(catalog.findActiveProductsByCategory("rosas", page)).thenReturn(expected);

        assertThat(service.listProducts("rosas", page)).isSameAs(expected);
        verify(catalog, never()).findActiveProducts(any());
    }

    @Test
    void getProductReturnsTheProduct() {
        Product product = product("ramo-primavera");
        when(catalog.findActiveBySlug("ramo-primavera")).thenReturn(Optional.of(product));

        assertThat(service.getProduct("ramo-primavera")).isEqualTo(product);
    }

    @Test
    void getProductFailsWhenMissing() {
        when(catalog.findActiveBySlug("nada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProduct("nada"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listCategoriesReturnsWhatThePortGives() {
        List<Category> categories = List.of(new Category(1L, "Rosas", "rosas", true, 1));
        when(catalog.findActiveCategories()).thenReturn(categories);

        assertThat(service.listCategories()).isEqualTo(categories);
    }
}