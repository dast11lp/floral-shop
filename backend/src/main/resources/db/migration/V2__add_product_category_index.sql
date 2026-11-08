-- Índice para buscar los productos de una categoría.
-- La clave primaria de product_category empieza por product_id, así que no sirve
-- para la pregunta contraria: "¿qué productos tiene esta categoría?".
-- (category_id, product_id) también cubre el join sin tocar la tabla.
CREATE INDEX idx_product_category_category ON product_category (category_id, product_id);