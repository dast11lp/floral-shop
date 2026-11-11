# 0009. Reglas de negocio en la capa de aplicación (enmienda del 0001)

**Estado:** aceptada, enmendada en parte por el 0010 (ubicación de los puertos), con precisiones del módulo `payment` · **Fecha:** 2026-11-03

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

## Precisiones (módulo `payment`, fase 3)
- **Qué puede validar un modelo:** su propia consistencia, es decir, lo que es cierto en cualquier caso de uso y no depende de configuración, del reloj ni de otros datos. Por ejemplo, que `DeliverySlot` no tenga más reservas que capacidad, o que un `Payment` tenga un monto mayor que cero.
- **La máquina de estados del pago vive en `domain`** (`PaymentStatus.canTransitionTo` y `Payment.transitionTo`). Es una regla de consistencia: un pago en un estado final (`PAID`, `FAILED`, `EXPIRED` o `CANCELLED`) no cambia nunca, venga el cambio de un webhook, del admin o de un job. Si se dejara en cada servicio, se repetiría en todos los que tocan pagos, que es justo el motivo de revisión de este ADR.
- **El modelo es estricto y el caso de uso decide.** Pasar al mismo estado o salir de uno final lanza `InvalidPaymentTransitionException`. Qué hacer con un evento repetido (por ejemplo, confirmar dos veces un pago ya pagado, que no debe duplicarse) lo decide el servicio de `application`, no el modelo.
- **Siguen en `application`** las reglas que dependen de configuración o de contexto: la política de cada medio (`PaymentMethodPolicy`: si está habilitado, retención del cupo y límites), quién puede confirmar un pago manual y el vencimiento de los pagos pendientes.