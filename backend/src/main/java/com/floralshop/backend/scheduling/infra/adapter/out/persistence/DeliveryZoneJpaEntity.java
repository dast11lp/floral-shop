package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla delivery_zone. Solo lectura por ahora: sin setters.
@Entity
@Table(name = "delivery_zone")
public class DeliveryZoneJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "margin_hours", nullable = false)
    private int marginHours;

    protected DeliveryZoneJpaEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public boolean isActive() { return active; }
    public int getMarginHours() { return marginHours; }
}