package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.scheduling.application.port.out.DeliverySettingsRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DeliverySettings;

// Adaptador de salida: lee los ajustes de entrega de la única fila de delivery_settings.
@Repository
class DeliverySettingsPersistenceAdapter implements DeliverySettingsRepositoryPort {

    private final DeliverySettingsJpaRepository jpa;

    DeliverySettingsPersistenceAdapter(DeliverySettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    // La migración V5 inserta la fila y un CHECK impide que haya otra: si falta, es un error de instalación.
    @Override
    public DeliverySettings get() {
        DeliverySettingsJpaEntity row = jpa.findById(DeliverySettingsJpaEntity.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de delivery_settings"));
        return new DeliverySettings(row.getDispatchMarginHours());
    }
}