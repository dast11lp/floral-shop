package com.floralshop.backend.scheduling.application.port.out;

import java.util.List;

import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Puerto de salida: zonas de entrega. Solo lectura por ahora. Lo implementará un adapter de persistencia (paso 3).
public interface DeliveryZoneRepositoryPort {

    // Solo las zonas activas, ordenadas por nombre.
    List<DeliveryZone> findActive();
}