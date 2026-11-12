package com.floralshop.backend.payment.infra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla payment_method_policy, que es de payment (ADR 0014, punto 1). Solo lectura por ahora:
// la consola que la edita llega en la fase 4.
@Entity
@Table(name = "payment_method_policy")
public class PaymentMethodPolicyJpaEntity {

    @Id
    private Short id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethodColumn method;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "hold_minutes", nullable = false)
    private int holdMinutes;

    @Column(nullable = false)
    private int priority;

    protected PaymentMethodPolicyJpaEntity() {
    }

    public Short getId() { return id; }
    public String getProvider() { return provider; }
    PaymentMethodColumn getMethod() { return method; }
    public boolean isEnabled() { return enabled; }
    public int getHoldMinutes() { return holdMinutes; }
    public int getPriority() { return priority; }
}
