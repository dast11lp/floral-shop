# 0001. Monolito modular con arquitectura hexagonal estricta

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
Es un proyecto de una sola persona, con un solo despliegue y un dominio acotado (catálogo, cupos de entrega, pedidos, pagos, reseñas, correos e identidad). Hay terceros que pueden cambiar (pasarela de pago, almacenamiento, correo, proveedor de nube) y partes del negocio, como el control de cupos, que deben poder probarse sin infraestructura.

## Decisión
Un único backend (Spring Boot) dividido en módulos, y cada módulo organizado como hexagonal:
- `domain`: reglas de negocio, entidades puras y puertos (interfaces).
- `application`: casos de uso y la API pública del módulo.
- `infra`: adaptadores (REST, JPA, S3, SES, Mercado Pago).

Reglas de dependencia, verificadas con ArchUnit o Spring Modulith:
1. `domain` no depende de Spring, JPA, el SDK de AWS ni de `application` o `infra`.
2. `application` depende solo de `domain`.
3. `infra` puede depender de `domain` y `application`, nunca al revés.
4. Un módulo solo usa la API pública de otro, nunca sus clases internas.
5. Cada módulo es dueño de sus tablas.
6. No hay ciclos entre módulos.

Las entidades del dominio son puras y las de JPA viven en `infra`, con mapeadores.

## Consecuencias
- **A favor:** cambiar de proveedor es escribir un adaptador; las reglas de negocio se prueban rápido y sin base de datos; las fronteras se hacen cumplir con pruebas, no con buena voluntad.
- **En contra:** más archivos y más indirección, y mapeo entre entidades de dominio y de JPA. Es el costo asumido del rigor.
- **Descartado:** microservicios (complejidad sin beneficio a este tamaño) y capas clásicas (se desordenan al crecer).
