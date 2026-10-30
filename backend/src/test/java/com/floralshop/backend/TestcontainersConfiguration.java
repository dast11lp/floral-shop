package com.floralshop.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection // Este objeto es una base de datos. Conecta la aplicación a él automáticamente
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17"); // versión de la imagen de PostgreSQL que deseas usar
    }
}