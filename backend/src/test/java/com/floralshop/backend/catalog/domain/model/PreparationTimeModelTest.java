package com.floralshop.backend.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

// Tiempo de preparación y cupos que ocupa (ADR 0012). Java puro.
class PreparationTimeModelTest {

    private ProductVariant variant(Integer hours, int slotUnits) {
        return new ProductVariant(1L, "Grande", new BigDecimal("130000"), true, 1, hours, slotUnits);
    }

    private Product product(int hours, ProductVariant variant) {
        return new Product(1L, "Ramo", "ramo", "Descripción", true, hours, List.of(variant), List.of());
    }

    @Test
    void variantWithoutOwnTimeUsesTheProductTime() {
        ProductVariant variant = variant(null, 1);

        assertThat(product(24, variant).preparationHoursOf(variant)).isEqualTo(24);
    }

    @Test
    void variantOwnTimeWinsOverTheProductTime() {
        ProductVariant variant = variant(48, 1);

        assertThat(product(24, variant).preparationHoursOf(variant)).isEqualTo(48);
    }

    @Test
    void variantOwnTimeCanBeZero() {
        // 0 es un valor propio ("ya está listo"), no "sin valor": no hereda
        ProductVariant variant = variant(0, 1);

        assertThat(product(24, variant).preparationHoursOf(variant)).isZero();
    }

    @Test
    void productRejectsNegativeTime() {
        assertThatThrownBy(() -> product(-1, variant(null, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void variantRejectsNegativeTime() {
        assertThatThrownBy(() -> variant(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void variantOccupiesAtLeastOneSlotUnit() {
        assertThatThrownBy(() -> variant(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- La regla, una sola vez ----

    @Test
    void ruleUsesTheProductTimeWhenTheVariantHasNone() {
        assertThat(ProductVariant.effectivePreparationHours(null, 24)).isEqualTo(24);
    }

    @Test
    void ruleKeepsTheVariantOwnTimeEvenIfZero() {
        assertThat(ProductVariant.effectivePreparationHours(0, 24)).isZero();
    }

    @Test
    void workloadInheritsTheProductTime() {
        assertThat(new VariantWorkload(1L, null, 24, 1).preparationHours()).isEqualTo(24);
    }

    @Test
    void workloadUsesItsOwnTime() {
        assertThat(new VariantWorkload(1L, 48, 24, 3).preparationHours()).isEqualTo(48);
    }

    @Test
    void workloadOccupiesAtLeastOneSlotUnit() {
        assertThatThrownBy(() -> new VariantWorkload(1L, null, 24, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cartLineRejectsQuantityNotGreaterThanZero() {
        assertThatThrownBy(() -> new CartLine(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}