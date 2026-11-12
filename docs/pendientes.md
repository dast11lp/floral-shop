# Pendientes y decisiones por revisar

Última actualización: 2026-11-12 (PR 1b de pagos: inicio del pago con Mercado Pago).

Cuando algo se resuelve, se mueve a "Resuelto" con la fecha y el PR. Si era una decisión de diseño, se escribe además un ADR.

## Decisiones de diseño por revisar

| # | Tema | Contexto y opciones | Cuándo revisarlo |
|---|---|---|---|
| 1 | Subcategorías | Hoy `category` es plana, y un producto puede estar en varias categorías (`product_category`). **A:** seguir plana. **B:** un nivel, con `parent_id` en `category` (migración V2); hay que decidir si pedir una categoría padre incluye los productos de sus hijas, y que "solo un nivel" lo haría cumplir un servicio. **C:** árbol libre (descartada). Recomendación: dejarla plana hasta hablar con la florista sobre cómo organiza su oferta | Antes de la fase 4 |
| 2 | Variante agotada visible | Hoy lo inactivo se oculta en la tienda (opción A). **B:** devolver también las inactivas con una marca, para mostrarlas deshabilitadas. **C:** columna `available` aparte de `active`. Inactiva (la florista la oculta) y agotada (no quedan) son conceptos distintos | Al diseñar el inventario, o si la florista pide mostrar agotados |
| 3 | Escritura del catálogo | Las entidades JPA no llevan setters. Decidir cómo se escribe (métodos en las entidades o entidades aparte) y cómo se actualiza `updated_at` (trigger o código) | Fase 4 |
| 4 | Consulta de inventario | La florista debe ver también lo inactivo y las existencias en 0. Requiere otro puerto, o más métodos que no filtren por `active` | Cuando se haga el inventario |
| 7 | Paginación compartida | `PageQuery` y `PagedResult` viven en `catalog/domain/model`. Si otro módulo los necesita, se mueven a `shared` y se ajustan las reglas de ArchUnit | Cuando otro módulo pagine |
| 15 | Fechas del producto en el dominio | `created_at` y `updated_at` están mapeadas en `ProductJpaEntity` (solo lectura), pero el modelo `Product` no las lleva. Añadirlas al dominio y al mapper cuando el inventario las necesite | Cuando se haga el inventario |
| 16 | Paginación por cursor | Hoy se pagina con `LIMIT/OFFSET`, que se vuelve lento en páginas muy lejanas. Con el catálogo de una floristería no se nota. Si algún día hubiera miles de productos, pasar a paginación por cursor | Solo si el catálogo crece mucho |
| 18 | Vista de administración de reservas | Hoy el cupo es solo un contador (`booked`). Las "reservas" que verá la administradora serán los pedidos agrupados por franja (fase 3 los crea, fase 4 los muestra), y cancelar un pedido debe liberar el cupo. Valorar una comprobación que cuadre `booked` con la suma de unidades de los pedidos activos | Fases 3 y 4 |
| 19 | Qué muestra la tienda del cupo de una franja | Hoy: solo `available` (sí o no), sin cifras, para no revelar cuánto vende la florista cada día. Hablarlo con ella: puede preferir mostrar la cantidad (`availableUnits`) o un estado ("últimos cupos"). Añadir un campo a la respuesta no rompe nada; quitarlo sí, así que se mantiene lo mínimo hasta decidir | Antes de la fase 5 |
| 21 | Decisiones de la tienda por hablar con la florista | Orden de las zonas (hoy alfabético: la tabla no tiene `sort_order`, así que ella no podría elegirlo). Productos por página (hoy 20 por defecto, máximo 100). Franjas llenas: se muestran como no disponibles; cerradas: se ocultan | Antes de la fase 5 |
| 22 | Puntos del ADR 0012 por hablar con la florista | Si los combos se activan al lanzar o después; qué zonas, con qué costo y margen (localidades, barrios o municipios cercanos como Chía); si habilita la transferencia manual o el contra entrega, y con qué límites; valores de arranque (2 horas de margen de despacho; el cupo apartado ya no es un valor a ojo: 36 minutos con fuentes, ADR 0013) | Antes de terminar la fase 3 |
| 23 | Días que no trabaja | Hoy todos los días cuentan para el tiempo de preparación (ADR 0012). Si ella lo pide, pasar a días hábiles: se cambia el servicio que calcula la fecha y se agrega una tabla con los días que no trabaja | Cuando ella lo pida |
| 24 | Detectar la zona desde la dirección | Hoy el cliente elige su zona de una lista (ADR 0012). Detectarla sola exige un servicio de mapas (geocodificación, con costo) y los límites de cada zona, y las direcciones llegan incompletas. Se agrega sin cambiar el modelo de zonas | Si elegir la zona a mano da problemas |
| 25 | Sedes en otras ciudades | Misma tienda y misma cuenta de cobro, con otra persona que arma y entrega en otra ciudad (ADR 0012, punto 12). Cada sede tendría sus zonas, franjas, márgenes y quizá sus tiempos. No se construye todavía; zonas, franjas y márgenes ya son datos, así que agregarlo es indicar a qué sede pertenecen | Cuando haya una segunda sede |
| 28 | Bandeja de salida de eventos | Los efectos hacia afuera (correo, aviso a la florista, PDF) salen por eventos guardados en una bandeja de salida (ADR 0014, punto 4). Opciones: Spring Modulith (verificar compatibilidad con Spring Boot 4.1), tabla y job propios, o solo después de guardar con un botón para reenviar | Al construir el correo |
| 29 | Dominio de la tienda | El correo de producción necesita un dominio propio con SPF, DKIM y DMARC (ADR 0014, punto 6). Elegirlo con la florista | Antes de producción |


## Tareas pendientes

| # | Tarea | Detalle |
|---|---|---|
| 9 | Guía de repaso para principiantes | Desactualizada: ArchUnit pasó a la fase 2 y faltan los ADR 0009 a 0014 (reglas en application, paquetes, transacciones, flujo de compra, pedidos, y pagos y correo) |
| 10 | Repaso de Flyway | Quedó pendiente de la fase 1 |
| 11 | `docker build` en el CI | Hoy un Dockerfile roto no lo detecta nadie |
| 14 | Spring Security | En la fase 4, `/api/v1/health` debe quedar público |
| 27 | El cupo apartado todavía no vence | Desde el PR #38, un pedido pendiente de pago retiene su cupo sin vencimiento. Desde el PR 1b el pedido ya guarda hasta cuándo retiene su cupo (`hold_expires_at`: 15 minutos al crearse, 36 al iniciar el pago, con tope de 51; ADR 0013, precisión del 2026-11-12), pero nadie lo libera todavía. Lo hace el job del tercer PR de pagos (ADR 0014), usando ese vencimiento. Como el vencimiento es del pedido, el job cubre también los pedidos que nunca inician un pago. Mientras tanto, en desarrollo, los pedidos de prueba dejan cupos ocupados; se limpian recreando la base | Con los pagos (fase 3) |
| 30 | Explicarle a la florista por qué Gmail no sirve en producción | Para la demostración, los correos salen de un Gmail personal (ADR 0014, punto 6). En producción no: el remitente sería un Gmail personal, tiene límites y no está pensado para una tienda. Preparar la explicación cuando toque |


## Por verificar (nadie lo ha confirmado)

- Comisiones de Mercado Pago y Wompi en Colombia, y qué tan rápido responde Nequi por Mercado Pago (aparece como banco dentro de PSE: confirmado el 2026-11-12).
- Si la cuenta de AWS de la florista recibe créditos de cuenta nueva.
- Soporte real de EC2 e IAM en Floci.
- Si las pasarelas exigen constitución legal para las llaves de producción.
- Cómo se servirán las fotos, y las condiciones de su cuenta de Nequi o banco.
- Límites gratuitos de Brevo y Resend en sus páginas oficiales, y que Gmail siga ofreciendo contraseñas de aplicación (ADR 0014).
- Que el vencimiento de 15 minutos de PSE aplica también a Checkout Pro, y los 21 minutos máximos de procesamiento de PSE (ADR 0013, fuentes no oficiales de ACH).

## Resuelto

| Qué | Dónde |
|---|---|
| Índice `product_category (category_id, product_id)` para buscar productos por categoría | Migración V2, sub-paso 2.5b |
| Problema N+1 al listar productos con variantes y categorías | `default_batch_fetch_size: 100` en `application.yaml`. Comprobado con una prueba que cuenta las consultas |
| Producto activo sin variantes activas | Se muestra con `available: false`, para que la tienda lo muestre como "no disponible por el momento". Decidido con la desarrolladora (2.6b) |
| `.gitignore` ampliado con llaves, Terraform, respaldos y archivos de frontend | PR `chore/gitignore-completo` |
| Fases 0, 1 y 2 marcadas como terminadas en `plan.md`. La diferencia de nombre entre `.env` y `.env.example` se descartó: la plantilla es coherente por sí sola | PR `docs/alinear-env-y-plan` |
| El CI levanta postgres, backend y nginx con Docker y prueba `/api/v1/health` a través de Nginx | PR `ci/docker-smoke-test` |
| Antelación mínima de pedidos (antes pendiente 20): se reemplaza por un tiempo de preparación en horas por producto y variante, y el tiempo del carrito con reglas que ajusta la florista | ADR 0012, PR `docs/flujo-de-compra` (2026-11-10) |
| Eventos internos (antes pendiente 5): no se usan por ahora; los pedidos llaman directo a los servicios públicos | ADR 0013, punto 4 (2026-11-11) |
| Transacción de pedido y reserva (antes pendiente 6): una sola transacción, declarada en `infra/config` de pedidos | ADR 0013, punto 1 (2026-11-11) |
| Claves foráneas entre módulos (antes pendiente 17): el pedido guarda ids y una copia de los datos, sin claves foráneas, con una prueba sobre `information_schema` | ADR 0013, punto 2 (2026-11-11) |
| Dueño de `shop_settings` (antes pendiente 26): se separa en una tabla de ajustes por módulo | ADR 0013, punto 3 (2026-11-11) |