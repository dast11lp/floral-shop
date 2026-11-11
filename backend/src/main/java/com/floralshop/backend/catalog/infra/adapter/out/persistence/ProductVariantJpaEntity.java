package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla product_variant. No guarda la referencia al producto:
// la relación se declara solo desde ProductJpaEntity, que es el lado que la usa.
@Entity
@Table(name = "product_variant")
public class ProductVariantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // null: la variante usa el tiempo de su producto.
    @Column(name = "preparation_hours")
    private Integer preparationHours;

    @Column(name = "slot_units", nullable = false)
    private int slotUnits;

    protected ProductVariantJpaEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    public Integer getPreparationHours() { return preparationHours; }
    public int getSlotUnits() { return slotUnits; }
}