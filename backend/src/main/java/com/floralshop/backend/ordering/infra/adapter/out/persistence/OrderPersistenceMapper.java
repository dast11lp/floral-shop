package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderStatus;

// Convierte entre el pedido del dominio y sus entidades JPA, en las dos direcciones.
// subtotal, total y lineTotal se guardan ya calculados por el dominio, para consultas y reportes.
class OrderPersistenceMapper {

    CustomerOrderJpaEntity toEntity(Order order) {
        return new CustomerOrderJpaEntity(order.publicCode(), OrderStatusColumn.valueOf(order.status().name()),
                order.customer().name(), order.customer().email(), order.customer().phone(),
                order.deliveryAddress(), order.zoneId(), order.zoneName(), order.shippingFee(),
                order.slotId(), order.slotDate(), order.slotStart(), order.slotEnd(),
                order.craftingHours(), order.slotUnits(), order.subtotal(), order.total(),
                order.lines().stream().map(this::toEntity).toList());
    }

    private OrderLineJpaEntity toEntity(OrderLine line) {
        return new OrderLineJpaEntity(line.variantId(), line.productName(), line.variantName(), line.unitPrice(),
                line.quantity(), line.craftingHours(), line.slotUnits(), line.lineTotal());
    }

    Order toDomain(CustomerOrderJpaEntity entity) {
        return new Order(entity.getId(), entity.getPublicCode(), OrderStatus.valueOf(entity.getStatus().name()),
                new CustomerContact(entity.getCustomerName(), entity.getCustomerEmail(), entity.getCustomerPhone()),
                entity.getDeliveryAddress(), entity.getZoneId(), entity.getZoneName(), entity.getShippingFee(),
                entity.getSlotId(), entity.getSlotDate(), entity.getSlotStart(), entity.getSlotEnd(),
                entity.getCraftingHours(), entity.getSlotUnits(),
                entity.getLines().stream().map(this::toDomain).toList());
    }

    private OrderLine toDomain(OrderLineJpaEntity line) {
        return new OrderLine(line.getVariantId(), line.getProductName(), line.getVariantName(), line.getUnitPrice(),
                line.getQuantity(), line.getCraftingHours(), line.getSlotUnits());
    }
}