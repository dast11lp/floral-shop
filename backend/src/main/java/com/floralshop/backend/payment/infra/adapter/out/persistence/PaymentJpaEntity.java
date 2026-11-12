package com.floralshop.backend.payment.infra.adapter.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Refleja la tabla payment, que es de payment. order_id es un dato, sin relación JPA con los pedidos
// (ADR 0013, punto 2). created_at lo pone la base de datos; resolved_at lo llenará el PR 2.
// Un intento se guarda primero sin cobro (sin referencia ni URL) y se completa después de crearlo en la pasarela.
@Entity
@Table(name = "payment")
public class PaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(nullable = false, updatable = false, length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private PaymentMethodColumn method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatusColumn status;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    protected PaymentJpaEntity() {
    }

    PaymentJpaEntity(Long id, Long orderId, String provider, PaymentMethodColumn method, PaymentStatusColumn status,
                     BigDecimal amount, String providerReference, String checkoutUrl) {
        this.id = id;
        this.orderId = orderId;
        this.provider = provider;
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.providerReference = providerReference;
        this.checkoutUrl = checkoutUrl;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getProvider() { return provider; }
    PaymentMethodColumn getMethod() { return method; }
    PaymentStatusColumn getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getProviderReference() { return providerReference; }
    public String getCheckoutUrl() { return checkoutUrl; }
}
