-- Nombres que dicen qué cubre cada tiempo (precisión del ADR 0012, 2026-11-11).
-- Solo cambia el nombre: los datos y las reglas siguen iguales. La V3 no se edita porque ya se aplicó.
--   crafting_hours        lo que tarda en elaborarse (antes preparation_hours)
--   dispatch_margin_hours empacar, salir e imprevistos; aplica a todo pedido (antes delivery_margin_hours)
--   travel_margin_hours   lo extra de llegar a esa zona (antes margin_hours)

ALTER TABLE product RENAME COLUMN preparation_hours TO crafting_hours;
ALTER TABLE product RENAME CONSTRAINT ck_product_preparation_hours TO ck_product_crafting_hours;

ALTER TABLE product_variant RENAME COLUMN preparation_hours TO crafting_hours;
ALTER TABLE product_variant RENAME CONSTRAINT ck_variant_preparation_hours TO ck_variant_crafting_hours;

ALTER TABLE shop_settings RENAME COLUMN delivery_margin_hours TO dispatch_margin_hours;
ALTER TABLE shop_settings RENAME CONSTRAINT ck_settings_delivery_margin_hours TO ck_settings_dispatch_margin_hours;

ALTER TABLE delivery_zone RENAME COLUMN margin_hours TO travel_margin_hours;
ALTER TABLE delivery_zone RENAME CONSTRAINT ck_zone_margin_hours TO ck_zone_travel_margin_hours;
