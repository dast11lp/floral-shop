# Pendientes y decisiones por revisar

Última actualización: 2026-11-04 (fase 2, sub-paso 2.5b).

Cuando algo se resuelve, se mueve a "Resuelto" con la fecha y el PR. Si era una decisión de diseño, se escribe además un ADR.

## Decisiones de diseño por revisar

| # | Tema | Contexto y opciones | Cuándo revisarlo |
|---|---|---|---|
| 1 | Subcategorías | Hoy `category` es plana, y un producto puede estar en varias categorías (`product_category`). **A:** seguir plana. **B:** un nivel, con `parent_id` en `category` (migración V2); hay que decidir si pedir una categoría padre incluye los productos de sus hijas, y que "solo un nivel" lo haría cumplir un servicio. **C:** árbol libre (descartada). Recomendación: dejarla plana hasta hablar con la florista sobre cómo organiza su oferta | Antes de la fase 4 |
| 2 | Variante agotada visible | Hoy lo inactivo se oculta en la tienda (opción A). **B:** devolver también las inactivas con una marca, para mostrarlas deshabilitadas. **C:** columna `available` aparte de `active`. Inactiva (la florista la oculta) y agotada (no quedan) son conceptos distintos | Al diseñar el inventario, o si la florista pide mostrar agotados |
| 3 | Escritura del catálogo | Las entidades JPA no llevan setters. Decidir cómo se escribe (métodos en las entidades o entidades aparte) y cómo se actualiza `updated_at` (trigger o código) | Fase 4 |
| 4 | Consulta de inventario | La florista debe ver también lo inactivo y las existencias en 0. Requiere otro puerto, o más métodos que no filtren por `active` | Cuando se haga el inventario |
| 5 | Eventos internos con publisher | Decidir con su propio ADR | Fase 3 |
| 6 | Transacción de pedido y reserva | El ADR 0011 declara la transacción en `infra`. Si crear un pedido y reservar cupo deben deshacerse juntos, la transacción tiene que abarcar ambos pasos. Si un pedido toca varias franjas, bloquearlas siempre en el mismo orden para evitar interbloqueos | Fase 3 |
| 7 | Paginación compartida | `PageQuery` y `PagedResult` viven en `catalog/domain/model`. Si otro módulo los necesita, se mueven a `shared` y se ajustan las reglas de ArchUnit | Cuando otro módulo pagine |

## Tareas pendientes

| # | Tarea | Detalle |
|---|---|---|
| 8 | `.gitignore` | Añadir: `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.keystore`, `.aws/`; Terraform (`*.tfplan`, `tfplan`, `override.tf`, `override.tf.json`, `*_override.tf`, `.terraformrc`, `terraform.rc`); respaldos (`*.dump`, `*.backup`, `*.bak`); frontend (`coverage/`, `*.tsbuildinfo`, `.sass-cache/`, `npm-debug.log*`, `yarn-error.log*`). **No** ignorar `*.sql` ni `.terraform.lock.hcl`. Rama sugerida: `chore/gitignore-completo` |
| 9 | Guía de repaso para principiantes | Desactualizada: ArchUnit pasó a la fase 2 y faltan los ADR 0009, 0010 y 0011 |
| 10 | Repaso de Flyway | Quedó pendiente de la fase 1 |
| 11 | `docker build` en el CI | Hoy un Dockerfile roto no lo detecta nadie |
| 12 | `plan.md` | Marcar la fase 1 como terminada |
| 13 | `.env.example` | Dice `POSTGRES_DB=tienda`, y el `.env` real usa `store` |
| 14 | Spring Security | En la fase 4, `/api/v1/health` debe quedar público |

## Por verificar (nadie lo ha confirmado)

- Comisiones y medios (Nequi, Efecty, PSE) de Mercado Pago y Wompi en Colombia.
- Si la cuenta de AWS de la florista recibe créditos de cuenta nueva.
- Soporte real de EC2 e IAM en Floci.
- Si las pasarelas exigen constitución legal para las llaves de producción.
- Cómo se servirán las fotos, y las condiciones de su cuenta de Nequi o banco.

## Resuelto

Nada todavía.