package com.floralshop.backend.ordering.application.port.out;

import com.floralshop.backend.ordering.domain.model.OrderSettings;

// Puerto de salida: los máximos por línea y por pedido. La tabla siempre tiene una sola fila.
public interface OrderSettingsRepositoryPort {

    OrderSettings get();
}