-- Pedidos del cliente invitado (ADR 0012 y 0013).
-- Sin claves foráneas hacia catalog ni scheduling (ADR 0013, punto 2): se guardan los ids como dato
-- y una copia de lo comprado, de la zona y de la franja, para que el pedido no cambie si ellos cambian.

-- "order" es palabra reservada en SQL: por eso customer_order.
CREATE TABLE customer_order (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_code      UUID           NOT NULL UNIQUE,
    status           VARCHAR(30)    NOT NULL
        CONSTRAINT ck_order_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'PAID_WITHOUT_SLOT')),
    customer_name    VARCHAR(120)   NOT NULL,
    customer_email   VARCHAR(254)   NOT NULL,
    customer_phone   VARCHAR(30)    NOT NULL,
    delivery_address VARCHAR(300)   NOT NULL,
    zone_id          BIGINT         NOT NULL,
    zone_name        VARCHAR(80)    NOT NULL,
    shipping_fee     NUMERIC(12, 2) NOT NULL CONSTRAINT ck_order_shipping_fee CHECK (shipping_fee >= 0),
    slot_id          BIGINT         NOT NULL,
    slot_date        DATE           NOT NULL,
    slot_start       TIME           NOT NULL,
    slot_end         TIME           NOT NULL,
    crafting_hours   INTEGER        NOT NULL CONSTRAINT ck_order_crafting_hours CHECK (crafting_hours >= 0),
    slot_units       INTEGER        NOT NULL CONSTRAINT ck_order_slot_units CHECK (slot_units > 0),
    subtotal         NUMERIC(12, 2) NOT NULL CONSTRAINT ck_order_subtotal CHECK (subtotal >= 0),
    total            NUMERIC(12, 2) NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_order_total CHECK (total = subtotal + shipping_fee)
);

-- Para que el job que libera cupos encuentre rápido los pedidos de una franja.
CREATE INDEX idx_customer_order_slot ON customer_order (slot_id);

CREATE TABLE order_line (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id       BIGINT         NOT NULL REFERENCES customer_order (id),  -- mismo módulo: sí lleva clave foránea
    variant_id     BIGINT         NOT NULL,
    product_name   VARCHAR(120)   NOT NULL,
    variant_name   VARCHAR(60)    NOT NULL,
    unit_price     NUMERIC(12, 2) NOT NULL CONSTRAINT ck_line_unit_price CHECK (unit_price >= 0),
    quantity       INTEGER        NOT NULL CONSTRAINT ck_line_quantity CHECK (quantity > 0),
    crafting_hours INTEGER        NOT NULL CONSTRAINT ck_line_crafting_hours CHECK (crafting_hours >= 0),
    slot_units     INTEGER        NOT NULL CONSTRAINT ck_line_slot_units CHECK (slot_units > 0),
    line_total     NUMERIC(12, 2) NOT NULL,
    CONSTRAINT ck_line_total CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_order_line_order ON order_line (order_id);
