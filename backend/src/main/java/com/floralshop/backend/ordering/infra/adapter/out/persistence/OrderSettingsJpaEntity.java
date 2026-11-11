package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla order_settings (una sola fila, id = 1), que es de ordering (ADR 0013, punto 3).
@Entity
@Table(name = "order_settings")
public class OrderSettingsJpaEntity {

    static final short SINGLE_ROW_ID = 1;

    @Id
    private Short id;

    @Column(name = "max_units_per_line", nullable = false)
    private int maxUnitsPerLine;

    @Column(name = "max_units_per_order", nullable = false)
    private int maxUnitsPerOrder;

    protected OrderSettingsJpaEntity() {
    }

    public Short getId() { return id; }
    public int getMaxUnitsPerLine() { return maxUnitsPerLine; }
    public int getMaxUnitsPerOrder() { return maxUnitsPerOrder; }
}