-- Datos de ejemplo SOLO para desarrollo local (perfil "local"). Todo es inventado.
-- Flyway ejecuta este archivo después de cada arranque, así que es idempotente:
-- los INSERT ignoran lo que ya existe, y las franjas se calculan desde hoy (hora de Bogotá).

INSERT INTO category (name, slug, active, sort_order) VALUES
    ('Rosas', 'rosas', true, 1),
    ('Girasoles', 'girasoles', true, 2),
    ('Cumpleaños', 'cumpleanos', true, 3),
    ('Categoría oculta', 'oculta', false, 4)
ON CONFLICT DO NOTHING;

-- crafting_hours: horas que tarda en armarse (ADR 0012).
INSERT INTO product (name, slug, description, active, crafting_hours) VALUES
    ('Ramo de rosas rojas', 'ramo-rosas-rojas', 'Rosas rojas de ejemplo', true, 4),
    ('Girasoles del campo', 'girasoles-del-campo', 'Girasoles de ejemplo', true, 6),
    ('Arreglo de cumpleaños', 'arreglo-cumpleanos', 'Arreglo de ejemplo', true, 24),
    ('Ramo sin variantes activas', 'ramo-sin-variantes', 'Se ve como no disponible', true, 4),
    ('Producto oculto', 'producto-oculto', 'No debe verse en la tienda', false, 4)
ON CONFLICT DO NOTHING;

INSERT INTO product_variant (product_id, name, price, active, sort_order)
SELECT p.id, v.name, v.price, v.active, v.sort_order
FROM product p
JOIN (VALUES
    ('ramo-rosas-rojas', 'Pequeño', 60000, true, 1),
    ('ramo-rosas-rojas', 'Mediano', 90000, true, 2),
    ('ramo-rosas-rojas', 'Grande', 130000, true, 3),
    ('girasoles-del-campo', 'Mediano', 75000, true, 1),
    ('girasoles-del-campo', 'Grande', 110000, true, 2),
    ('arreglo-cumpleanos', 'Único', 85000, true, 1),
    ('ramo-sin-variantes', 'Único', 50000, false, 1)
) AS v(product_slug, name, price, active, sort_order) ON v.product_slug = p.slug
ON CONFLICT DO NOTHING;

-- El ramo grande tarda más que el del producto y ocupa 2 cupos de la franja (ADR 0012).
-- Se repite en cada arranque, así que es un UPDATE y no parte del INSERT.
UPDATE product_variant v SET crafting_hours = 48, slot_units = 2
FROM product p
WHERE v.product_id = p.id AND p.slug = 'ramo-rosas-rojas' AND v.name = 'Grande';

INSERT INTO product_category (product_id, category_id)
SELECT p.id, c.id
FROM (VALUES
    ('ramo-rosas-rojas', 'rosas'),
    ('ramo-sin-variantes', 'rosas'),
    ('girasoles-del-campo', 'girasoles'),
    ('girasoles-del-campo', 'cumpleanos'),
    ('arreglo-cumpleanos', 'cumpleanos'),
    ('producto-oculto', 'oculta')
) AS l(product_slug, category_slug)
JOIN product p ON p.slug = l.product_slug
JOIN category c ON c.slug = l.category_slug
ON CONFLICT DO NOTHING;

INSERT INTO delivery_zone (name, shipping_fee, active) VALUES
    ('Centro', 8000, true),
    ('Norte', 12000, true),
    ('Sur', 12000, true),
    ('Zona inactiva', 15000, false)
ON CONFLICT DO NOTHING;

-- Margen de cada zona: el tiempo extra de llegar a ella (ADR 0012).
UPDATE delivery_zone SET travel_margin_hours = 1 WHERE name = 'Norte';
UPDATE delivery_zone SET travel_margin_hours = 2 WHERE name = 'Sur';

-- Dos franjas al día durante los próximos 7 días.
INSERT INTO delivery_slot (slot_date, start_time, end_time, capacity, booked, is_open)
SELECT d::date, s.start_time, s.end_time, 4, 0, true
FROM generate_series((now() AT TIME ZONE 'America/Bogota')::date + 1,
                     (now() AT TIME ZONE 'America/Bogota')::date + 7,
                     interval '1 day') AS d,
     (VALUES ('09:00'::time, '11:00'::time), ('14:00'::time, '16:00'::time)) AS s(start_time, end_time)
ON CONFLICT DO NOTHING;

-- Para ver los dos casos: mañana a las 14:00 está llena, y pasado mañana a las 9:00 está cerrada.
-- (Estos dos UPDATE se repiten en cada arranque.)
UPDATE delivery_slot SET booked = capacity
WHERE slot_date = (now() AT TIME ZONE 'America/Bogota')::date + 1 AND start_time = '14:00';

UPDATE delivery_slot SET is_open = false
WHERE slot_date = (now() AT TIME ZONE 'America/Bogota')::date + 2 AND start_time = '09:00';