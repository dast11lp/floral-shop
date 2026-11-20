package com.floralshop.backend.payment.infra.adapter.out.persistence;

// Los valores de la columna status_reason de payment. Copia del enum del dominio a propósito, como PaymentStatusColumn.
enum PaymentStatusReasonColumn {
    GATEWAY_REJECTED, CREATION_UNCONFIRMED
}
