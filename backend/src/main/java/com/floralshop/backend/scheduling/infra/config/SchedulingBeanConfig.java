package com.floralshop.backend.scheduling.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;

@Configuration
class SchedulingBeanConfig {
    
    @Bean
    ReserveSlotService reserveSlotService(DeliverySlotRepositoryPort slots) {
        return new TransactionalReserveSlotService(slots);
    }
}