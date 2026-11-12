package com.floralshop.backend.payment.infra.adapter.out.persistence;

// Los valores de la columna status de payment. Copia del enum del dominio a propósito, como PaymentMethodColumn.
enum PaymentStatusColumn {
    PENDING, PAID, FAILED, EXPIRED, CANCELLED
}
