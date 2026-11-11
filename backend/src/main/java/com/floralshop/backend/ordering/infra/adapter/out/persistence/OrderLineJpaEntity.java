package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla order_line. variant_id es solo un dato, sin clave foránea al catálogo (ADR 0013).
@Entity
@Table(name = "order_line")
public class OrderLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_id", nullable = false)
    private long variantId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_name", nullable = false)
    private String variantName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "crafting_hours", nullable = false)
    private int craftingHours;

    @Column(name = "slot_units", nullable = false)
    private int slotUnits;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderLineJpaEntity() {
    }

    OrderLineJpaEntity(long variantId, String productName, String variantName, BigDecimal unitPrice,
                       int quantity, int craftingHours, int slotUnits, BigDecimal lineTotal) {
        this.variantId = variantId;
        this.productName = productName;
        this.variantName = variantName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.craftingHours = craftingHours;
        this.slotUnits = slotUnits;
        this.lineTotal = lineTotal;
    }

    public Long getId() { return id; }
    public long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public int getCraftingHours() { return craftingHours; }
    public int getSlotUnits() { return slotUnits; }
    public BigDecimal getLineTotal() { return lineTotal; }
}