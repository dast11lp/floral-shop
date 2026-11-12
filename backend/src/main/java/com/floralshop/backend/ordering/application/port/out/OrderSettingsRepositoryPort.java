package com.floralshop.backend.ordering.application.port.out;

import com.floralshop.backend.ordering.domain.model.OrderSettings;

// Puerto de salida: los ajustes de pedidos (máximos y tiempos de retención). La tabla siempre tiene una sola fila.
public interface OrderSettingsRepositoryPort {

    OrderSettings get();
}