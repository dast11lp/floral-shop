package com.floralshop.backend.scheduling.application.port.out;

import java.util.List;
import java.util.Optional;

import com.floralshop.backend.scheduling.domain.model.DeliveryZone;

// Puerto de salida: zonas de entrega. Solo lectura por ahora.
public interface DeliveryZoneRepositoryPort {

    // Solo las zonas activas, ordenadas por nombre.
    List<DeliveryZone> findActive();

    // Una zona activa por su id. Vacío si no existe o está inactiva.
    Optional<DeliveryZone> findActiveById(long id);
}