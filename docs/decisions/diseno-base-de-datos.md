# Diseño de la base de datos: propuesta para aprobar

**Estado:** borrador. Léelo con calma, dime qué cambiar, y solo entonces se pega el SQL. Mientras la V1 no se haya fusionado en `main`, se puede cambiar sin costo.

**Método:** primero la base de datos (SQL con Flyway). Después las clases de Java la mapean. Hibernate solo **comprueba** (`ddl-auto: validate`), nunca crea tablas.

---

## 1. Lo que decidimos

| Tema | Decisión |
|---|---|
| Producto y catálogo | `product` es el diseño que se vende. El **catálogo no es una tabla**: es la lista de productos activos |
| Categorías | Un producto puede estar en varias (cumpleaños, rosas...). **"A tu gusto" es una categoría** |
| Tamaños | Un producto tiene **variantes** (Pequeño, Mediano, Grande...), cada una con su precio. Son **datos**, no una lista fija: ella los cambia sin tocar código |
| "A tu gusto" | Un producto por presupuesto ("Hasta $80.000"), como variantes. El detalle va en una **nota** del pedido (fase 3) |
| Cupo | Cuenta **arreglos (unidades)**, no pedidos |
| Línea del pedido | Apunta a la **variante** y copia nombre y precio del día (fase 3) |
| Comprador y destinatario | Se guardan por separado, con mensaje de tarjeta (fase 3) |
| Clientes | Tabla `customer` con consentimiento para escribirles (fase 3) |
| Existencias e inventario | **Sí, pero por niveles y después del MVP** (sección 6) |
| Capacidad | Editable por ella desde el panel de administración (sección 5) |

**Pendiente de tu confirmación:** el costo de envío depende de la **zona**. ¿El **cupo** también depende de la zona? Propongo que **no** en el MVP: el cupo es general por franja, y las zonas solo definen el costo de envío. Si quieres cupo por zona, cambia el diseño (se dice abajo).

---

## 2. El mapa

```
category ──< product_category >── product ──< product_variant
                                                    │  (fase 3: la línea del pedido apunta aquí)
delivery_zone      delivery_slot       shop_settings
(zonas y envío)    (franjas y cupo)    (límites que ella edita)
```

`──<` significa "uno a muchos". `product_category` es la tabla intermedia que permite que un producto esté en varias categorías.

**Tres tablas o cuatro:** la base clásica de un carrito son tres (`product`, `orders`, `order_item`). Aquí hay más porque tu negocio tiene categorías, variantes y franjas. Cada vez que aparece una relación "muchos a muchos" o un dato que se repite, nace una tabla.

---

## 3. La V1: qué se crea ahora (fase 2)

Son las tablas de "catálogo y franjas". Pedidos, pagos, reseñas, clientes y fotos van en sus fases.

| Tabla | Para qué | Columnas principales |
|---|---|---|
| `category` | Agrupar productos | name, slug (URL), active, sort_order |
| `product` | El diseño que se vende | name, slug (URL única), description, active, fechas |
| `product_category` | Une productos y categorías | product_id, category_id |
| `product_variant` | Tamaño o presupuesto, con precio | product_id, name, price, active, sort_order |
| `delivery_zone` | Zona de entrega | name, shipping_fee, active |
| `delivery_slot` | Franja de entrega con cupo | fecha, hora inicio y fin, capacity, booked, is_open |
| `shop_settings` | Límites que ella edita (una sola fila) | max_units_per_line, max_units_per_order, default_slot_capacity |

**Reglas que la base hace cumplir, siempre:**
- El precio y el costo de envío no pueden ser negativos.
- `booked` no puede superar a `capacity`. **Es la regla estrella.**
- La franja debe terminar después de empezar, y no puede haber dos franjas que empiecen a la misma hora el mismo día.
- Dos productos no pueden tener la misma URL (`slug`). Un producto no puede tener dos variantes con el mismo nombre.

**Reglas que van en Spring** (son política y pueden cambiar): el máximo de unidades por línea y por pedido, y los mensajes amables al cliente.

---

## 4. El SQL propuesto

Archivo: `backend/src/main/resources/db/migration/V1__create_catalog_and_scheduling.sql`
(**dos guiones bajos** después de `V1`)

```sql
CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE product (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    description TEXT         NOT NULL DEFAULT '',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE product_category (
    product_id  BIGINT NOT NULL REFERENCES product (id),
    category_id BIGINT NOT NULL REFERENCES category (id),
    PRIMARY KEY (product_id, category_id)
);

CREATE TABLE product_variant (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT         NOT NULL REFERENCES product (id),
    name       VARCHAR(60)    NOT NULL,
    price      NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT uq_variant_product_name UNIQUE (product_id, name)
);

CREATE TABLE delivery_zone (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(80)    NOT NULL UNIQUE,
    shipping_fee NUMERIC(12, 2) NOT NULL CHECK (shipping_fee >= 0),
    active       BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE delivery_slot (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slot_date  DATE    NOT NULL,
    start_time TIME    NOT NULL,
    end_time   TIME    NOT NULL,
    capacity   INTEGER NOT NULL CHECK (capacity >= 0),
    booked     INTEGER NOT NULL DEFAULT 0 CHECK (booked >= 0),
    is_open    BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_slot_booked_le_capacity CHECK (booked <= capacity),
    CONSTRAINT ck_slot_time_order CHECK (end_time > start_time),
    CONSTRAINT uq_slot_date_start UNIQUE (slot_date, start_time)
);

CREATE TABLE shop_settings (
    id                    SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    max_units_per_line    INTEGER     NOT NULL DEFAULT 10 CHECK (max_units_per_line > 0),
    max_units_per_order   INTEGER     NOT NULL DEFAULT 20 CHECK (max_units_per_order > 0),
    default_slot_capacity INTEGER     NOT NULL DEFAULT 4  CHECK (default_slot_capacity >= 0),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO shop_settings DEFAULT VALUES;
```

**Cómo leer lo menos obvio:**
- `GENERATED ALWAYS AS IDENTITY`: la base numera sola (1, 2, 3...).
- `REFERENCES product (id)`: una **llave foránea**. La base no deja crear una variante de un producto que no existe.
- `NUMERIC(12, 2)`: número exacto, para dinero. Nunca `double`.
- `TIMESTAMPTZ`: fecha y hora con zona, guardada en UTC.
- `shop_settings` tiene **una sola fila**: la regla `CHECK (id = 1)` impide que haya más. Los números 10, 20 y 4 son valores de ejemplo, que ella cambiará.
- `capacity >= 0` permite poner 0 para cerrar una franja sin borrarla. `is_open` también la cierra.

**La reserva de cupos, ahora por unidades:**
```sql
UPDATE delivery_slot
SET booked = booked + :unidades
WHERE id = :id AND is_open AND booked + :unidades <= capacity;
```
Si no afecta ninguna fila, no hay cupo. Es una sola instrucción, así que dos clientes simultáneos no pueden sobrevender.

---

## 5. Cómo cambiará ella la capacidad "a voluntad"

La capacidad **vive en la base de datos, no en el código**. Por eso se puede cambiar sin programar:

| Qué quiere ella | Qué se edita | Dónde |
|---|---|---|
| "Mañana solo puedo con 3 arreglos en la franja de la mañana" | `delivery_slot.capacity` | Panel de administración |
| "Cierro el sábado por la tarde" | `delivery_slot.is_open = false` | Panel |
| "El máximo por pedido ahora es 15" | `shop_settings.max_units_per_order` | Panel |
| "Las franjas nuevas nacen con cupo 6" | `shop_settings.default_slot_capacity` | Panel |

**Si baja la capacidad por debajo de lo ya reservado**, la base lo rechaza (`booked <= capacity`). El panel mostrará un mensaje claro: "ya hay N reservados".

**Cuándo existirá ese panel:** la **API** de administración llega en la fase 4 y la **pantalla** en la fase 5. Hasta entonces los valores se cargan con datos de ejemplo.

**Mejora opcional futura:** una "plantilla semanal" de franjas (lunes 9-11, capacidad 4...) que genere las franjas de cada semana. No se necesita para el MVP.

---

## 6. Inventario: por niveles, después del MVP

Tu idea es buena, y se puede construir **sin tocar la V1**: las tablas nuevas se **agregan** con migraciones posteriores. Lo que sí cuesta caro es cambiar tablas que ya tienen datos, y la V1 está pensada para no necesitarlo.

**Una advertencia honesta del oficio:** una florista **sustituye** flores según lo que consiga ese día, y las flores **se dañan**. Un inventario estricto con recetas ("este ramo usa 12 rosas") se desactualiza rápido. Por eso va por niveles, y tú decides hasta dónde llegar.

| Nivel | Qué es | Qué agrega | Esfuerzo |
|---|---|---|---|
| **1 (MVP)** | Capacidad por franja, editable | Nada nuevo: ya está en la V1 | Ya hecho |
| **2** | Existencias por variante: "quedan 5 arreglos grandes" | Una columna `stock` en la variante, y un descuento atómico como el de los cupos | Bajo |
| **3** | Inventario de **insumos** y recetas | `supply`, `stock_movement`, `recipe_item` | Alto |

**El nivel 3, para que veas cómo sería:**
- `supply`: el insumo (rosa roja, follaje, cinta, jarrón), con su unidad y sus existencias.
- `stock_movement`: **un libro de movimientos**: compra, venta, merma, ajuste. Cada cambio de existencias deja un renglón; nunca se edita el saldo "a mano". Así hay historial y se puede auditar.
- `recipe_item`: qué insumos y cuántos lleva cada variante.
- **Disponibilidad** de una variante = el insumo más escaso. Si el ramo pide 12 rosas y quedan 30, caben 2 ramos.
- **Máximos que dependen de los insumos:** el límite por línea sería el menor entre el máximo configurado y lo que alcanzan los insumos.
- Se reserva con la misma técnica atómica del cupo: `UPDATE ... WHERE stock >= cantidad`.

**Mi recomendación:** hacer el nivel 1 ahora, dejar el 2 y el 3 como "después del MVP", y decidir con ella cuál le sirve de verdad. Lo agregaré a la lista de opcionales del plan.

---

## 7. Lo que la V1 no incluye (a propósito)

| Falta | Por qué | Cuándo |
|---|---|---|
| Fotos (`product_image`) | Depende de S3 | Fase 4 |
| Clientes, pedidos y líneas | Dependen de los pagos | Fase 3 |
| Pagos y eventos | Fase de pagos | Fase 3 |
| Usuarios y roles | Fase de seguridad | Fase 4 |
| Reseñas | Dependen de pedidos entregados | Fase 6 |
| Inventario | Después del MVP | Opcional |

---

## 8. Preguntas pendientes

1. **¿El cupo depende de la zona?** Mi propuesta: no, es general por franja. (Cambiarlo implica una tabla más: capacidad por franja y zona.)
2. **¿Los valores de ejemplo de `shop_settings` te sirven (10 por línea, 20 por pedido, 4 de cupo por franja)?** Son solo de arranque, ella los cambia después.
3. **¿Algún nombre o campo que quieras cambiar?** Todo está en inglés (convención de código); lo que ve el cliente se traduce en la pantalla.

---

## 9. Cómo aplicarlo cuando lo apruebes

1. **Borra** el archivo `V1__create_product_and_delivery_slot.sql` si lo creaste.
2. **Crea** `V1__create_catalog_and_scheduling.sql` en `backend/src/main/resources/db/migration/` con el SQL de la sección 4.
3. Si ya habías arrancado la app con la V1 vieja, recrea la base vacía (los datos de prueba no importan):
   ```powershell
   docker compose down -v
   docker compose up -d
   ```
4. Arranca con el perfil `local` y comprueba las tablas:
   ```powershell
   docker compose exec postgres psql -U tienda -d tienda -c "\dt"
   ```
   Deben aparecer las 7 tablas, más `flyway_schema_history`.
5. **Prueba que la base se defiende**: intenta insertar una franja con `booked` mayor que `capacity`. Debe fallar con `violates check constraint "ck_slot_booked_le_capacity"`.
