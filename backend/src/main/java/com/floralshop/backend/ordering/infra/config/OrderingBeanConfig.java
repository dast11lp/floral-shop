package com.floralshop.backend.ordering.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.application.port.out.OrderSettingsRepositoryPort;
import com.floralshop.backend.ordering.application.service.CreateOrderService;
import com.floralshop.backend.ordering.application.service.OrderHoldService;
import com.floralshop.backend.ordering.application.service.OrderQueryService;
import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;

@Configuration
class OrderingBeanConfig {

    @Bean
    CreateOrderService createOrderService(OrderSettingsRepositoryPort settings, OrderRepositoryPort orders,
                                          CatalogQueryService catalog, SchedulingQueryService scheduling,
                                          DeliveryOptionsService deliveryOptions, ReserveSlotService reserveSlot,
                                          Clock clock) {
        return new TransactionalCreateOrderService(settings, orders, catalog, scheduling, deliveryOptions, reserveSlot,
                clock);
    }

    @Bean
    OrderQueryService orderQueryService(OrderRepositoryPort orders) {
        return new OrderQueryService(orders);
    }

    @Bean
    OrderHoldService orderHoldService(OrderRepositoryPort orders) {
        return new OrderHoldService(orders);
    }
}