package com.floralshop.backend.catalog.infra.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.catalog.domain.model.PageQuery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Adaptador de entrada: traduce HTTP a llamadas al servicio. No decide nada.
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogQueryService catalog;

    public CatalogController(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    // Lista paginada. Con ?category=rosas, solo los productos de esa categoría.
    // Las páginas empiezan en 0. @Min y @Max devuelven un 400 claro si piden algo fuera de rango.
    @GetMapping("/products")
    public PageResponse<ProductResponse> listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(
                catalog.listProducts(category, new PageQuery(page, size)),
                ProductResponse::from);
    }

    // Un producto por su slug. Si no existe o está inactivo, el servicio lanza ProductNotFoundException (404).
    @GetMapping("/products/{slug}")
    public ProductResponse getProduct(@PathVariable String slug) {
        return ProductResponse.from(catalog.getProduct(slug));
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalog.listCategories().stream().map(CategoryResponse::from).toList();
    }
}