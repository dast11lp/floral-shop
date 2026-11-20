package com.floralshop.backend.payment.infra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla payment_settings (una sola fila, id = 1), que es de payment (migración V10).
@Entity
@Table(name = "payment_settings")
public class PaymentSettingsJpaEntity {

    static final short SINGLE_ROW_ID = 1;

    @Id
    private Short id;

    @Column(name = "max_attempts_per_order", nullable = false)
    private int maxAttemptsPerOrder;

    protected PaymentSettingsJpaEntity() {
    }

    public Short getId() { return id; }
    public int getMaxAttemptsPerOrder() { return maxAttemptsPerOrder; }
}
