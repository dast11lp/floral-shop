package com.floralshop.backend.catalog.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.application.service.CartWorkloadService;
import com.floralshop.backend.catalog.application.service.CatalogQueryService;

@Configuration
class CatalogBeanConfig {

    @Bean
    CatalogQueryService catalogQueryService(CatalogRepositoryPort catalog) {
        return new CatalogQueryService(catalog);
    }

    @Bean
    CartWorkloadService cartWorkloadService(CatalogRepositoryPort catalog) {
        return new CartWorkloadService(catalog);
    }
}