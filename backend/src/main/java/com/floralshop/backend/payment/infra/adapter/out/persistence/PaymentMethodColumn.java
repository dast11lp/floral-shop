package com.floralshop.backend.payment.infra.adapter.out.persistence;

// Los valores de la columna method. Es una copia del enum del dominio a propósito:
// renombrar un medio en el dominio no debe cambiar en silencio lo que hay guardado.
// El adaptador traduce entre los dos, y el CHECK de la tabla acepta solo estos valores.
enum PaymentMethodColumn {
    CARD, PSE, NEQUI, CASH_POINT, MANUAL_TRANSFER, CASH_ON_DELIVERY
}
