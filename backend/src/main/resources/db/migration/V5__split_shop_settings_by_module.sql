-- Cada módulo es dueño de sus ajustes (ADR 0013, punto 3; ADR 0001, regla 5).
-- shop_settings se separa en dos tablas de una sola fila y se copian los valores actuales.

-- scheduling: margen de despacho y capacidad con la que nacen las franjas.
CREATE TABLE delivery_settings (
    id                    SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    dispatch_margin_hours INTEGER     NOT NULL DEFAULT 2
        CONSTRAINT ck_delivery_settings_dispatch_margin_hours CHECK (dispatch_margin_hours >= 0),
    default_slot_capacity INTEGER     NOT NULL DEFAULT 4
        CONSTRAINT ck_delivery_settings_default_slot_capacity CHECK (default_slot_capacity >= 0),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO delivery_settings (id, dispatch_margin_hours, default_slot_capacity)
SELECT 1, dispatch_margin_hours, default_slot_capacity FROM shop_settings;

-- ordering: máximos de unidades por línea y por pedido.
CREATE TABLE order_settings (
    id                  SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    max_units_per_line  INTEGER     NOT NULL DEFAULT 10
        CONSTRAINT ck_order_settings_max_units_per_line CHECK (max_units_per_line > 0),
    max_units_per_order INTEGER     NOT NULL DEFAULT 20
        CONSTRAINT ck_order_settings_max_units_per_order CHECK (max_units_per_order > 0),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO order_settings (id, max_units_per_line, max_units_per_order)
SELECT 1, max_units_per_line, max_units_per_order FROM shop_settings;

DROP TABLE shop_settings;
