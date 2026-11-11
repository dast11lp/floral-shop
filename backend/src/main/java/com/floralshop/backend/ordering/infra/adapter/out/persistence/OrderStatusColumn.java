package com.floralshop.backend.ordering.infra.adapter.out.persistence;

// Los valores de la columna status. Es una copia del enum del dominio a propósito:
// renombrar un estado en el dominio no debe cambiar en silencio lo que hay guardado.
// El mapper traduce entre los dos, y el CHECK de la tabla acepta solo estos valores.
enum OrderStatusColumn {
    PENDING_PAYMENT, CONFIRMED, CANCELLED, PAID_WITHOUT_SLOT
}