package com.floralshop.backend.ordering.domain.model;

// Estados de un pedido (ADR 0012). Este PR solo crea pedidos en PENDING_PAYMENT;
// los demás los usarán los pagos y el job que libera cupos.
public enum OrderStatus {
    PENDING_PAYMENT,   // el cupo está apartado mientras se paga
    CONFIRMED,         // el pago se aprobó: la florista lo arma y lo entrega
    CANCELLED,         // el pago se rechazó o venció: el cupo se liberó
    PAID_WITHOUT_SLOT  // el pago se aprobó cuando el cupo ya se había liberado: decide la florista
}