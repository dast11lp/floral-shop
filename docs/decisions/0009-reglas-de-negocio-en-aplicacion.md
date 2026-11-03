# 0009. Reglas de negocio en la capa de aplicación (enmienda del 0001)

**Estado:** aceptada · **Fecha:** 2026-11-03

## Contexto
El ADR 0001 ponía las reglas de negocio dentro de los objetos de `domain`. Al empezar la fase 2 (módulo `scheduling`) se evaluó la alternativa de ponerlas en servicios de `application`, conservando la separación entre el modelo de dominio y las entidades de JPA (con mapeadores). La reserva de cupos se decidirá en la base de datos de forma atómica, así que un `reserve()` rico en el dominio duplicaría esa regla.

## Decisión
- `domain`: modelos de datos del negocio (sin Spring, JPA ni AWS, e inmutables cuando sea posible) y puertos (interfaces). Sin reglas de negocio.
- `application`: casos de uso **y reglas de negocio**, en servicios. Un servicio por caso de uso.
- `infra`: adaptadores. Las entidades de JPA siguen separadas de los modelos de dominio, con mapeadores (`toDomain` y `toEntity`).
- Las seis reglas de dependencia del ADR 0001 no cambian.

## Consecuencias
- **A favor:** las reglas están en un lugar visible, los servicios se prueban con Mockito, el dominio sigue libre de frameworks y encaja con la reserva atómica en SQL.
- **En contra:** los modelos no se protegen solos de estados inválidos. Se mitiga con servicios pequeños, los `CHECK` de la base de datos y las pruebas.
- **Cuándo se revisa:** si aparecen reglas duplicadas entre servicios o servicios que crecen sin control.
- **Descartado:** dominio rico (la opción original del 0001).