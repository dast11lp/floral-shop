# floral-shop

Proyecto de portafolio: tienda online para una florista de Bogotá.

**Stack:** Spring Boot, Next.js (React), PostgreSQL, Docker, AWS.

## Estado
En desarrollo. Fases 0, 1 y 2 terminadas; fase 3 (pedidos y pagos) en curso.

Lo que ya funciona en el backend:
- Catálogo con variantes, categorías y tiempo de elaboración (`GET /api/v1/products`).
- Zonas y franjas de entrega con cupos (`GET /api/v1/delivery-zones`, `GET /api/v1/delivery-slots`).
- Franjas que alcanzan para un carrito, según su tiempo de elaboración y los márgenes de entrega (`POST /api/v1/delivery-options`).
- Pedido como invitado, con el cupo de la franja apartado en la misma transacción (`POST /api/v1/orders`).

Siguiente: pagos (pasarela, webhook y liberación de cupos vencidos). El detalle está en `docs/plan.md` y los pendientes en `docs/pendientes.md`.

## Documentación
- Plan general: `docs/plan.md`
- Decisiones de arquitectura: `docs/decisions/`

## Licencia
Todos los derechos reservados. Ver `LICENSE`.
