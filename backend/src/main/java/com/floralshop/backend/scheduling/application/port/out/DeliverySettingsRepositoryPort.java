package com.floralshop.backend.scheduling.application.port.out;

import com.floralshop.backend.scheduling.domain.model.DeliverySettings;

// Puerto de salida: los ajustes de entrega. La tabla siempre tiene una sola fila.
public interface DeliverySettingsRepositoryPort {

    DeliverySettings get();
}