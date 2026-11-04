package com.floralshop.backend.scheduling.application.port.out;

import java.util.Optional;

import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Puerto: lo que el negocio necesita para guardar y leer franjas.
// Es solo una interfaz. La implementación con JPA vive en infra (sub-paso 2.4).
public interface DeliverySlotRepositoryPort {

    // Busca una franja y la bloquea hasta que termine la transacción, para que dos
    // peticiones simultáneas no lean el mismo cupo a la vez. Devuelve vacío si no existe.
    Optional<DeliverySlot> findByIdForUpdate(long id);

    // Guarda la franja con sus valores actuales y devuelve la versión guardada.
    DeliverySlot save(DeliverySlot slot);
}