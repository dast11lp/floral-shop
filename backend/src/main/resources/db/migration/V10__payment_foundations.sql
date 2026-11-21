-- Cimientos del PR 2 de pagos (ADR 0016, sección F). Prepara la base para las máquinas de estado,
-- el aviso de la pasarela y el job; ninguna regla de negocio vive aquí, solo los datos que la sostienen.

-- ordering: el pedido pagado en revisión que ya soltó su franja (ADR 0016, A.2).
ALTER TABLE customer_order DROP CONSTRAINT ck_order_status;
ALTER TABLE customer_order
    ADD CONSTRAINT ck_order_status CHECK (status IN
        ('PENDING_PAYMENT', 'PENDING_PAYMENT_NO_SLOT', 'CONFIRMED', 'CANCELLED', 'PAID_WITHOUT_SLOT'));

-- no_slot_since: cuándo entró en PENDING_PAYMENT_NO_SLOT. Se fija una sola vez, en esa transición, y no se
-- reinicia: la alerta de 24 horas de la conciliación se cuenta desde aquí (ADR 0016, A.17). Se conserva
-- cuando el pedido sale de ese estado, como historial.
ALTER TABLE customer_order
    ADD COLUMN no_slot_since TIMESTAMPTZ,
    ADD CONSTRAINT ck_order_no_slot_since
        CHECK (status <> 'PENDING_PAYMENT_NO_SLOT' OR no_slot_since IS NOT NULL);

-- payment: el reembolso total de un pago aprobado (ADR 0016, A.4).
ALTER TABLE payment DROP CONSTRAINT ck_payment_status;
ALTER TABLE payment
    ADD CONSTRAINT ck_payment_status CHECK (status IN
        ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED'));

-- provider_status y provider_status_detail: el último estado que informó la pasarela, guardado aparte del
-- estado interno (ADR 0016, D.1). Van los dos porque el detalle decide (por ejemplo, processing / in_process).
-- Sin CHECK: son valores de la pasarela, y uno desconocido se guarda igual para poder alertar (A.14).
ALTER TABLE payment
    ADD COLUMN provider_status        VARCHAR(50),
    ADD COLUMN provider_status_detail VARCHAR(50);

-- status_reason: por qué el pago está en su estado, distinto del estado de la pasarela (ADR 0016, D.1 y P5).
-- Por ahora, los dos motivos de FAILED. Los motivos de un pago que sigue PENDING (A.2, A.19, A.20) llegan con
-- el job, en su propia migración. Los FAILED anteriores a esta migración quedan sin motivo: no se sabe cuál fue.
ALTER TABLE payment
    ADD COLUMN status_reason VARCHAR(30)
        CONSTRAINT ck_payment_status_reason CHECK (status_reason IN ('GATEWAY_REJECTED', 'CREATION_UNCONFIRMED'));

-- checkout_expires_at: hasta cuándo se puede pagar el cobro en la pasarela (ADR 0016, A.18, A.19 y A.23).
-- Null mientras el cobro no existe, o en los medios manuales.
ALTER TABLE payment
    ADD COLUMN checkout_expires_at TIMESTAMPTZ;

-- Para contar los intentos de un pedido (A.9) y buscar sus pagos sin recorrer la tabla.
CREATE INDEX ix_payment_order ON payment (order_id);

-- payment: sus ajustes, editables sin tocar el código (ADR 0016, A.9; ADR 0001, regla 5: cada módulo es
-- dueño de sus tablas). Una sola fila, como order_settings.
-- max_attempts_per_order: cuántos intentos con cobro creado puede tener un pedido. 5 es un valor de arranque,
-- sin datos que lo respalden; se ajusta con datos reales.
CREATE TABLE payment_settings (
    id                     SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    max_attempts_per_order INTEGER     NOT NULL DEFAULT 5
        CONSTRAINT ck_payment_settings_max_attempts_per_order CHECK (max_attempts_per_order > 0),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO payment_settings (id) VALUES (1);
