package com.floralshop.backend.ordering.infra.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.ordering.application.port.out.OrderSettingsRepositoryPort;
import com.floralshop.backend.ordering.domain.model.OrderSettings;

// Adaptador de salida: lee los máximos de la única fila de order_settings.
@Repository
class OrderSettingsPersistenceAdapter implements OrderSettingsRepositoryPort {

    private final OrderSettingsJpaRepository jpa;

    OrderSettingsPersistenceAdapter(OrderSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    // La migración V5 inserta la fila y un CHECK impide que haya otra: si falta, es un error de instalación.
    @Override
    public OrderSettings get() {
        OrderSettingsJpaEntity row = jpa.findById(OrderSettingsJpaEntity.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de order_settings"));
        return new OrderSettings(row.getMaxUnitsPerLine(), row.getMaxUnitsPerOrder());
    }
}