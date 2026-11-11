-- Tiempo de preparación, cupos que ocupa cada variante y márgenes de entrega (ADR 0012).
-- Todo en horas enteras. La API entrega las horas tal cual y el front decide cómo mostrarlas.

-- Tiempo de preparación del producto: obligatorio.
-- El DEFAULT solo rellena los productos que ya existen; después se quita,
-- para que un producto nuevo tenga que decir siempre cuánto tarda.
ALTER TABLE product
    ADD COLUMN preparation_hours INTEGER NOT NULL DEFAULT 24
        CONSTRAINT ck_product_preparation_hours CHECK (preparation_hours >= 0);
ALTER TABLE product ALTER COLUMN preparation_hours DROP DEFAULT;

-- Tiempo de la variante: opcional. Si es NULL, la variante usa el del producto.
-- Cupos que ocupa: cuántos cupos de una franja gasta cada unidad de la variante.
ALTER TABLE product_variant
    ADD COLUMN preparation_hours INTEGER
        CONSTRAINT ck_variant_preparation_hours CHECK (preparation_hours >= 0),
    ADD COLUMN slot_units INTEGER NOT NULL DEFAULT 1
        CONSTRAINT ck_variant_slot_units CHECK (slot_units > 0);

-- Margen de la zona: el tiempo extra de llegar a ella. 0 es "no suma nada".
ALTER TABLE delivery_zone
    ADD COLUMN margin_hours INTEGER NOT NULL DEFAULT 0
        CONSTRAINT ck_zone_margin_hours CHECK (margin_hours >= 0);

-- Margen general: empacar, salir e imprevistos. Lo edita la florista; arranca en 2 horas.
ALTER TABLE shop_settings
    ADD COLUMN delivery_margin_hours INTEGER NOT NULL DEFAULT 2
        CONSTRAINT ck_settings_delivery_margin_hours CHECK (delivery_margin_hours >= 0);
