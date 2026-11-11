package com.floralshop.backend.ordering.infra.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;

// Lo que ve el cliente de su pedido. code es su referencia; el id interno no se expone.
// No devuelve correo, teléfono ni dirección: el cliente ya los conoce y son datos personales.
public record OrderResponse(UUID code, String status, String zoneName, LocalDate slotDate, LocalTime slotStart,
                            LocalTime slotEnd, int craftingHours, List<Line> lines, BigDecimal subtotal,
                            BigDecimal shippingFee, BigDecimal total) {

    public record Line(String productName, String variantName, BigDecimal unitPrice, int quantity,
                       BigDecimal lineTotal) {

        static Line from(OrderLine line) {
            return new Line(line.productName(), line.variantName(), line.unitPrice(), line.quantity(),
                    line.lineTotal());
        }
    }

    static OrderResponse from(Order order) {
        return new OrderResponse(order.publicCode(), order.status().name(), order.zoneName(), order.slotDate(),
                order.slotStart(), order.slotEnd(), order.craftingHours(),
                order.lines().stream().map(Line::from).toList(),
                order.subtotal(), order.shippingFee(), order.total());
    }
}