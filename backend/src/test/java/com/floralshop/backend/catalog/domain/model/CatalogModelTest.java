package com.floralshop.backend.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

// Comprueba las reglas de consistencia de los modelos. Java puro.
class CatalogModelTest {

    private ProductVariant variant(String price) {
        return new ProductVariant(1L, "Mediano", new BigDecimal(price), true, 1);
    }

    @Test
    void categoryRejectsBlankName() {
        assertThatThrownBy(() -> new Category(1L, " ", "rosas", true, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void categoryRejectsBlankSlug() {
        assertThatThrownBy(() -> new Category(1L, "Rosas", "", true, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void variantRejectsNegativePrice() {
        assertThatThrownBy(() -> variant("-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void variantAcceptsZeroPrice() {
        assertThat(variant("0").price()).isEqualByComparingTo("0");
    }

    @Test
    void productRejectsBlankName() {
        assertThatThrownBy(() -> new Product(1L, " ", "ramo", "Descripción", true, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productRejectsBlankSlug() {
        assertThatThrownBy(() -> new Product(1L, "Ramo", "", "Descripción", true, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productCopiesItsLists() {
        List<ProductVariant> variants = new ArrayList<>(List.of(variant("50000")));
        Product product = new Product(1L, "Ramo", "ramo", "Descripción", true, variants, List.of());

        variants.add(variant("80000")); // si el producto no copiara la lista, vería este cambio

        assertThat(product.variants()).hasSize(1);
    }
}