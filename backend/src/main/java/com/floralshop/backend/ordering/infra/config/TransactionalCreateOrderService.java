package com.floralshop.backend.ordering.infra.config;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.application.port.out.OrderSettingsRepositoryPort;
import com.floralshop.backend.ordering.application.service.CreateOrderService;
import com.floralshop.backend.ordering.domain.model.NewOrder;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;

// Misma lógica que CreateOrderService, dentro de una transacción (ADR 0011 y 0013, punto 1).
// La reserva de cupo de scheduling se une a esta transacción: si guardar el pedido falla,
// el cupo apartado también se deshace. No añade lógica.
class TransactionalCreateOrderService extends CreateOrderService {

    TransactionalCreateOrderService(OrderSettingsRepositoryPort settings, OrderRepositoryPort orders,
                                    CatalogQueryService catalog, SchedulingQueryService scheduling,
                                    DeliveryOptionsService deliveryOptions, ReserveSlotService reserveSlot,
                                    Clock clock) {
        super(settings, orders, catalog, scheduling, deliveryOptions, reserveSlot, clock);
    }

    @Override
    @Transactional
    public Order create(NewOrder request) {
        return super.create(request);
    }
}