package com.floralshop.backend.payment.infra.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.floralshop.backend.payment.application.port.out.PaymentSettingsRepositoryPort;

// Adaptador de salida: lee los ajustes de la única fila de payment_settings.
@Repository
class PaymentSettingsPersistenceAdapter implements PaymentSettingsRepositoryPort {

    private final PaymentSettingsJpaRepository jpa;

    PaymentSettingsPersistenceAdapter(PaymentSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    // La migración V10 inserta la fila y un CHECK impide que haya otra: si falta, es un error de instalación.
    @Override
    public int maxAttemptsPerOrder() {
        return jpa.findById(PaymentSettingsJpaEntity.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de payment_settings"))
                .getMaxAttemptsPerOrder();
    }
}
