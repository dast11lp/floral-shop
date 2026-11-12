package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

// Refleja la tabla customer_order ("order" es palabra reservada en SQL).
// Las líneas se guardan junto con el pedido (cascade).
@Entity
@Table(name = "customer_order")
public class CustomerOrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_code", nullable = false, updatable = false)
    private UUID publicCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatusColumn status;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "zone_id", nullable = false)
    private long zoneId;

    @Column(name = "zone_name", nullable = false)
    private String zoneName;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "slot_id", nullable = false)
    private long slotId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_start", nullable = false)
    private LocalTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalTime slotEnd;

    @Column(name = "crafting_hours", nullable = false)
    private int craftingHours;

    @Column(name = "slot_units", nullable = false)
    private int slotUnits;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // Hasta cuándo retiene la franja y su tope (ADR 0012). La extensión se hace con una consulta
    // de actualización en CustomerOrderJpaRepository, no cambiando esta entidad.
    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Column(name = "hold_limit_at", nullable = false, updatable = false)
    private Instant holdLimitAt;

    // La rellena la base con now(): Hibernate nunca la escribe.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id ASC")
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    protected CustomerOrderJpaEntity() {
    }

    CustomerOrderJpaEntity(UUID publicCode, OrderStatusColumn status, String customerName, String customerEmail,
                           String customerPhone, String deliveryAddress, long zoneId, String zoneName,
                           BigDecimal shippingFee, long slotId, LocalDate slotDate, LocalTime slotStart,
                           LocalTime slotEnd, int craftingHours, int slotUnits, BigDecimal subtotal,
                           BigDecimal total, Instant holdExpiresAt, Instant holdLimitAt,
                           List<OrderLineJpaEntity> lines) {
        this.publicCode = publicCode;
        this.status = status;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.shippingFee = shippingFee;
        this.slotId = slotId;
        this.slotDate = slotDate;
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.craftingHours = craftingHours;
        this.slotUnits = slotUnits;
        this.subtotal = subtotal;
        this.total = total;
        this.holdExpiresAt = holdExpiresAt;
        this.holdLimitAt = holdLimitAt;
        this.lines.addAll(lines);
    }

    public Long getId() { return id; }
    public UUID getPublicCode() { return publicCode; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public Instant getHoldLimitAt() { return holdLimitAt; }
    public OrderStatusColumn getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public long getZoneId() { return zoneId; }
    public String getZoneName() { return zoneName; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public long getSlotId() { return slotId; }
    public LocalDate getSlotDate() { return slotDate; }
    public LocalTime getSlotStart() { return slotStart; }
    public LocalTime getSlotEnd() { return slotEnd; }
    public int getCraftingHours() { return craftingHours; }
    public int getSlotUnits() { return slotUnits; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLineJpaEntity> getLines() { return lines; }
}