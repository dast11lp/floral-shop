-- Pagos de los pedidos (ADR 0003, 0013 y 0014). Es del módulo payment.
-- order_id es el id del pedido guardado como dato, sin clave foránea hacia ordering (ADR 0013, punto 2).
-- Hasta cuándo se guarda la franja no vive aquí sino en el pedido (V9): vencer el cupo es asunto del pedido (ADR 0012).
CREATE TABLE payment (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id           BIGINT         NOT NULL,
    provider           VARCHAR(30)    NOT NULL,
    method             VARCHAR(30)    NOT NULL
        CONSTRAINT ck_payment_method
            CHECK (method IN ('CARD', 'PSE', 'NEQUI', 'CASH_POINT', 'MANUAL_TRANSFER', 'CASH_ON_DELIVERY')),
    status             VARCHAR(20)    NOT NULL
        CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED')),
    amount             NUMERIC(12, 2) NOT NULL CONSTRAINT ck_payment_amount CHECK (amount > 0),
    -- El id del cobro en la pasarela (en Mercado Pago, el de la order). Null en los manuales.
    provider_reference VARCHAR(100),
    -- Adónde se manda al cliente para pagar. Se guarda para devolver el mismo si lo pide otra vez.
    checkout_url       VARCHAR(500),
    -- created_at y resolved_at sirven para medir cuánto tardan los pagos por medio (ADR 0013, punto 5).
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    resolved_at        TIMESTAMPTZ,
    CONSTRAINT uq_payment_provider_reference UNIQUE (provider, provider_reference)
);

-- Un pedido nunca tiene dos pagos pendientes a la vez (ADR 0014, punto 3).
CREATE UNIQUE INDEX uq_payment_one_pending_per_order ON payment (order_id) WHERE status = 'PENDING';
