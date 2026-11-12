-- Hasta cuándo un pedido pendiente de pago retiene su franja (ADR 0012: vencer el cupo es asunto del pedido).
-- Es del módulo ordering.

-- Cuánto tiempo y con qué tope, editables por la florista (order_settings es de ordering, ADR 0013 punto 3):
-- payment_start_minutes: minutos para empezar a pagar desde que se crea el pedido. 15, igual que la
--   vigencia del cobro en Mercado Pago (ADR 0013, punto 5).
-- max_hold_minutes: tope total desde que se crea el pedido. 51 = 15 para empezar a pagar + 36 del peor
--   caso documentado de un pago que sí se aprueba (ADR 0013, punto 5). Nadie retiene una franja más que eso.
ALTER TABLE order_settings
    ADD COLUMN payment_start_minutes INTEGER NOT NULL DEFAULT 15
        CONSTRAINT ck_order_settings_payment_start_minutes CHECK (payment_start_minutes > 0),
    ADD COLUMN max_hold_minutes      INTEGER NOT NULL DEFAULT 51
        CONSTRAINT ck_order_settings_max_hold_minutes CHECK (max_hold_minutes > 0),
    ADD CONSTRAINT ck_order_settings_hold_order CHECK (max_hold_minutes >= payment_start_minutes);

-- hold_expires_at: hasta cuándo se retiene la franja ahora. Cada intento de pago la extiende.
-- hold_limit_at: el tope, fijado al crear el pedido. hold_expires_at nunca lo pasa.
-- Los pedidos que ya existían reciben los valores de arranque contados desde su creación.
ALTER TABLE customer_order
    ADD COLUMN hold_expires_at TIMESTAMPTZ,
    ADD COLUMN hold_limit_at   TIMESTAMPTZ;

UPDATE customer_order
SET hold_expires_at = created_at + INTERVAL '15 minutes',
    hold_limit_at   = created_at + INTERVAL '51 minutes';

ALTER TABLE customer_order
    ALTER COLUMN hold_expires_at SET NOT NULL,
    ALTER COLUMN hold_limit_at SET NOT NULL,
    ADD CONSTRAINT ck_order_hold CHECK (hold_expires_at <= hold_limit_at);

-- Para que el job que libera franjas (PR 3) encuentre rápido los pedidos pendientes vencidos.
CREATE INDEX ix_customer_order_pending_hold ON customer_order (hold_expires_at) WHERE status = 'PENDING_PAYMENT';
