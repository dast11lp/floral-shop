package com.floralshop.backend.shared.infra.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// El reloj de la aplicación, en la zona horaria de la tienda (Colombia).
// El servidor corre en UTC: sin una zona explícita, "hoy" cambiaría a las 7 p. m. hora de Bogotá.
@Configuration
class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}