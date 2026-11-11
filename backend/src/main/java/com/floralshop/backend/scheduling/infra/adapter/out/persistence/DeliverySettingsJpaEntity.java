package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja solo las columnas de entrega de shop_settings (una sola fila, id = 1).
// Las demás columnas (máximos por línea y por pedido) las mapeará el módulo que las use;
// ver el pendiente 26 sobre quién es dueño de shop_settings.
@Entity
@Table(name = "shop_settings")
public class DeliverySettingsJpaEntity {

    static final short SINGLE_ROW_ID = 1;

    @Id
    private Short id;

    @Column(name = "delivery_margin_hours", nullable = false)
    private int deliveryMarginHours;

    protected DeliverySettingsJpaEntity() {
    }

    public Short getId() { return id; }
    public int getDeliveryMarginHours() { return deliveryMarginHours; }
}