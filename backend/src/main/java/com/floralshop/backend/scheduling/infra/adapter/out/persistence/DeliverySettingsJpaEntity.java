package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla delivery_settings (una sola fila, id = 1), que es de scheduling (ADR 0013, punto 3).
// Solo mapea lo que el módulo usa hoy: el margen de despacho.
@Entity
@Table(name = "delivery_settings")
public class DeliverySettingsJpaEntity {

    static final short SINGLE_ROW_ID = 1;

    @Id
    private Short id;

    @Column(name = "dispatch_margin_hours", nullable = false)
    private int dispatchMarginHours;

    protected DeliverySettingsJpaEntity() {
    }

    public Short getId() { return id; }
    public int getDispatchMarginHours() { return dispatchMarginHours; }
}