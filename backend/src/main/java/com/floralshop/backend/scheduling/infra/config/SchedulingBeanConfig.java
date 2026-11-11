package com.floralshop.backend.scheduling.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.catalog.application.service.CartWorkloadService;
import com.floralshop.backend.scheduling.application.port.out.DeliverySettingsRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;
import com.floralshop.backend.scheduling.application.service.DeliveryTimeService;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;

@Configuration
class SchedulingBeanConfig {
    
    @Bean
    ReserveSlotService reserveSlotService(DeliverySlotRepositoryPort slots) {
        return new TransactionalReserveSlotService(slots);
    }

    @Bean
    SchedulingQueryService schedulingQueryService(DeliverySlotRepositoryPort slots,
                                                  DeliveryZoneRepositoryPort zones, Clock clock) {
        return new SchedulingQueryService(slots, zones, clock);
    }

    @Bean
    DeliveryOptionsService deliveryOptionsService(CartWorkloadService cart, DeliveryTimeService deliveryTime,
                                                  SchedulingQueryService scheduling) {
        return new DeliveryOptionsService(cart, deliveryTime, scheduling);
    }

    @Bean
    DeliveryTimeService deliveryTimeService(DeliveryZoneRepositoryPort zones,
                                            DeliverySettingsRepositoryPort settings, Clock clock) {
        return new DeliveryTimeService(zones, settings, clock);
    }
}