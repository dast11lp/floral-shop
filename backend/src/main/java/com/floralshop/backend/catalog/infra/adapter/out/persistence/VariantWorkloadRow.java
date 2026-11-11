package com.floralshop.backend.catalog.infra.adapter.out.persistence;

// Proyección de Spring Data: solo las cuatro columnas que necesita el carrito, sin cargar entidades.
// Los nombres de los métodos coinciden con los alias de la consulta.
interface VariantWorkloadRow {

    Long getVariantId();

    Integer getVariantPreparationHours(); // null: hereda el del producto

    Integer getProductPreparationHours();

    Integer getSlotUnits();
}