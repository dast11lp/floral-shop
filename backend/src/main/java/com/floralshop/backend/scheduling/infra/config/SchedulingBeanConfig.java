package com.floralshop.backend.scheduling.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.port.out.DeliveryZoneRepositoryPort;
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
}