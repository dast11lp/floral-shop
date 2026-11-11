package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.math.BigDecimal;

// Proyección de Spring Data para el pedido. Los nombres coinciden con los alias de la consulta.
interface OrderableVariantRow {

    Long getVariantId();

    String getProductName();

    String getVariantName();

    BigDecimal getPrice();

    Integer getVariantCraftingHours(); // null: hereda el del producto

    Integer getProductCraftingHours();

    Integer getSlotUnits();
}