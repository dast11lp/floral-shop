package com.floralshop.backend.ordering.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Un pedido. Guarda copias de la zona y de la franja, igual que de las variantes, para que el
// comprobante muestre lo que se compró aunque el catálogo o las franjas cambien (ADR 0013, punto 2).
// publicCode es lo que el cliente usa para consultar su pedido; el id interno no se expone.
// craftingHours y slotUnits son los del carrito completo, calculados por CartWorkloadService.
// holdExpiresAt: hasta cuándo retiene su franja mientras está pendiente de pago (ADR 0012: vencer el cupo
// es asunto del pedido). Cada intento de pago la extiende, pero nunca más allá de holdLimitAt.
public record Order(Long id, UUID publicCode, OrderStatus status, CustomerContact customer,
                    String deliveryAddress, long zoneId, String zoneName, BigDecimal shippingFee,
                    long slotId, LocalDate slotDate, LocalTime slotStart, LocalTime slotEnd,
                    int craftingHours, int slotUnits, List<OrderLine> lines,
                    Instant holdExpiresAt, Instant holdLimitAt) {

    public Order {
        Objects.requireNonNull(publicCode, "El código público es obligatorio");
        Objects.requireNonNull(status, "El estado es obligatorio");
        Objects.requireNonNull(customer, "El cliente es obligatorio");
        Objects.requireNonNull(deliveryAddress, "La dirección es obligatoria");
        Objects.requireNonNull(zoneName, "El nombre de la zona es obligatorio");
        Objects.requireNonNull(shippingFee, "El costo de envío es obligatorio");
        Objects.requireNonNull(slotDate, "La fecha de la franja es obligatoria");
        Objects.requireNonNull(slotStart, "La hora de inicio es obligatoria");
        Objects.requireNonNull(slotEnd, "La hora de fin es obligatoria");
        Objects.requireNonNull(holdExpiresAt, "El vencimiento de la franja es obligatorio");
        Objects.requireNonNull(holdLimitAt, "El tope de la franja es obligatorio");
        if (holdExpiresAt.isAfter(holdLimitAt)) {
            throw new IllegalArgumentException("El vencimiento de la franja no puede pasar del tope");
        }
        if (deliveryAddress.isBlank()) {
            throw new IllegalArgumentException("La dirección no puede estar vacía");
        }
        if (shippingFee.signum() < 0) {
            throw new IllegalArgumentException("El costo de envío no puede ser negativo");
        }
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Un pedido tiene al menos una línea");
        }
    }

    public BigDecimal subtotal() {
        return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal total() {
        return subtotal().add(shippingFee);
    }
}