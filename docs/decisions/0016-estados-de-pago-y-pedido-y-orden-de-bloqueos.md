# 0016. Estados de pago y pedido, y orden de bloqueos (enmienda prevista del 0003, 0012, 0013 y 0014)

**Estado:** propuesta, borrador para revisión. Tiene secciones condicionadas por decisiones pendientes (sección G). Todavía no enmienda ningún ADR: la reconciliación con el 0003, 0012, 0013 y 0014 se hace después de aprobar este documento. · **Fecha:** 2026-11-12

## Contexto
Antes de construir el aviso de Mercado Pago (PR 2 de pagos) se revisó cómo cambian de estado un pago y un pedido. La revisión encontró:

- **Modelos incompletos.** El pedido tiene cuatro estados y ninguna regla de transición. El pago no tiene un estado para el reembolso.
- **Contradicciones entre ADR.** Sobre qué hacer cuando un pago se rechaza o vence (0012 punto 1.6, 0012 punto 9, 0014 punto 3), sobre los avisos de un pago que ya no está pendiente (0014 punto 5) y sobre cuántos intentos de pago tiene un pedido (0014 punto 3 frente a las precisiones del 0013 y del 0014).
- **Una carrera en el código del PR 1b.** Al iniciar un pago, el estado del pedido se revisa fuera de la transacción, y si la retención no se puede extender, el intento sigue igual. Un aviso que confirma el pedido al mismo tiempo podría dejar un segundo cobro para un pedido ya pagado.
- **Los estados de Mercado Pago no corresponden uno a uno con los nuestros.** La API de Orders tiene más estados que nuestro pago, y algunos no son definitivos.

### Cómo leer este documento
- **Decisión de negocio:** la aprobó el negocio. Cambiarla es otra decisión.
- **Regla de arquitectura:** cómo debe comportarse el sistema para cumplir las decisiones. Está aprobada como concepto; que el código la cumpla se comprueba al implementar.
- **Detalle de implementación:** orientativo. Puede cambiar sin tocar las decisiones ni las reglas.
- **Pendiente (Pn):** falta una definición. La parte del documento que depende de ella no es definitiva. Las alternativas se listan sin elegir. Si hay una recomendación técnica, se indica como **recomendación sin aprobar**, que no es una decisión.
- **Comprobación (Vn):** algo que solo se sabe probando en el sandbox de Mercado Pago. Cada una indica si ya tiene resultado. Las migraciones de la base de datos se escriben siempre "migración Vn", para no confundirlas con las comprobaciones.
- **Regla operativa provisional:** una regla adoptada con la evidencia disponible, sin garantía documental del proveedor. Se indica como tal y dice qué la haría revisar.

## Definiciones

**Estado interno y estado del proveedor.**
- El **estado interno** representa nuestra decisión sobre el pago: el que deciden nuestras reglas.
- El **estado del proveedor** representa lo que informó Mercado Pago.
- Son cosas distintas y no se corresponden uno a uno. Ejemplo: estado interno `PENDING` mientras Mercado Pago informa `processing / in_process`.

**Estados de una order de Mercado Pago: dos niveles.** Mercado Pago informa cada order con dos campos de niveles distintos:
- **`status`**: el estado de la order (por ejemplo, `processed`).
- **`status_detail`**: el detalle dentro de ese estado (por ejemplo, `accredited`).

Un detalle no es un estado ni está a su mismo nivel. En este documento se escriben juntos como `status / status_detail`. Así, `processed / accredited` significa estado `processed` con detalle `accredited`.

Mercado Pago publica dos páginas de estados de order que **no coinciden**: la de **Checkout Pro vía Orders** (nuestra integración) y la de **Checkout API vía Orders** (otra integración). Las garantías de una no se trasladan a la otra, y lo observado en nuestro sandbox tampoco es una garantía documental.

| `status` | Checkout Pro (página oficial, consultada el 2026-11-12) | Checkout API (solo referencia) | Visto en nuestro sandbox (Checkout Pro) |
|---|---|---|---|
| `created` | `created` | — | `created` |
| `processing` | `in_process`, `pending_review_manual` | — | `in_process` |
| `processed` | `accredited`, `partially_refunded` | — | `accredited` |
| `action_required` | `waiting_capture` | incluye `waiting_retry` | **`waiting_retry`** |
| `failed` | varios detalles de rechazo (por ejemplo, `rejected_by_issuer`, `insufficient_amount`, `max_attempts_exceeded`) | `failed`, `expired` | **`failed`, `expired`** |
| `refunded` | `refunded` | — | — |
| `canceled` | `canceled` ("cancelada o expiró") | `canceled`, `expired` | `canceled`, `expired` |

*(La columna de Checkout API es solo referencia y se vuelve a contrastar con su página cuando se use.)*

- **El motivo del rechazo está en la transacción** (`transactions.payments[].status_detail`), no en la order.
- **Una order puede tener varias transacciones.** El rechazo de una transacción no cierra la order: en el sandbox, la order quedó `action_required / waiting_retry` y una segunda transacción la aprobó en el mismo checkout (V1).

El ciclo documentado se describe con los estados, no con los detalles:
- `created → processing → processed`;
- `created → canceled`;
- `created → failed`;
- `processed → refunded`.

**No se ha confirmado si `failed` es un estado terminal en Checkout Pro vía Orders.** Es una incertidumbre documentada que no bloquea la implementación (V1). Se maneja con reglas operativas provisionales (A.18 a A.21).

**Dos vencimientos distintos.** No son el mismo evento:
1. **Vencimiento del cobro (checkout) en Mercado Pago.** El cobro se crea con `expiration_time` = `PT15M` (ADR 0014, precisión del 2026-11-12). En el sandbox, el enlace dejó de funcionar a los 15 minutos, y la API informó `canceled / expired` unos 2,5 minutos después.
2. **Vencimiento de la retención de la franja.**
   - Vive en el pedido: `hold_expires_at`, con tope en `hold_limit_at` (ADR 0013, precisión del 2026-11-12).
   - Empieza en 15 minutos.
   - Cada intento la extiende a "ahora + los minutos de su opción" (36 con tarjeta).
   - Nunca pasa del tope de 51 minutos desde la creación del pedido.

**Cómo se ve el vencimiento del cobro** (observado en el sandbox):
- si la order no tuvo transacciones, queda `canceled / expired`;
- si tuvo transacciones rechazadas (estaba en `waiting_retry`), queda **`failed / expired`**.

En los dos casos, el cambio llegó entre 2,5 y 2,7 minutos después del vencimiento (V1).

El vencimiento del cobro no cancela el pedido; lo cancela el vencimiento de la retención (A.12).

## Decisión

### A. Decisiones de negocio (aprobadas)

1. **Pago aprobado, pedido confirmado.** Si el pago se aprueba mientras el pedido está pendiente de pago, el pedido queda confirmado y conserva su franja (ADR 0012, punto 1.5).
2. **Pago en revisión cuando vence la retención (C2).** C2 aplica **exactamente a estos dos pares** de estado y detalle de Mercado Pago:
   - `processing / in_process`;
   - `processing / pending_review_manual` (incorporado el 2026-11-13, P7 caso 2).

   Si vence la retención y la consulta de estado a Mercado Pago devuelve uno de esos dos pares:
   - el pago sigue `PENDING`;
   - el pedido pasa a `PENDING_PAYMENT_NO_SLOT`;
   - se libera la franja.

   Si después Mercado Pago confirma el pago, el pedido pasa a `PAID_WITHOUT_SLOT` y la florista coordina la entrega a mano. No se cancela el pedido ni se le reasigna la franja automáticamente.

   - **Por qué esos dos:** representan pagos todavía sin resolución, que no se pueden cancelar (la cancelación solo admite `created` y `action_required`; V2) y cuya duración no controlamos. Mantener la franja indefinidamente iría contra el ADR 0013 (punto 5: los pagos en revisión no bloquean el cupo).
   - **No se generaliza** a cualquier detalle futuro de `processing`. Un detalle que no esté en esta lista no aplica C2: se trata como un resultado no reconocido, y la franja no se libera (A.13). Incorporarlo a C2 sería una decisión nueva.
   - Qué pasa con los demás estados del proveedor al vencer la retención está en A.13, A.14 y A.15.
   - **Efectos parecidos por otras causas (2026-11-14).** A.19 y A.20 también liberan la franja y dejan el pedido `PENDING_PAYMENT_NO_SLOT` con el pago `PENDING`, en otros casos. Son decisiones aparte, con su propio motivo y sus propios mensajes (A.23). **No amplían la lista de C2 ni usan su mensaje de "en revisión".**
3. **Pago aprobado sin franja.** Una aprobación que llega cuando la franja ya se liberó deja el pedido en `PAID_WITHOUT_SLOT` (ADR 0012, punto 10). No se reasigna la franja: la florista gestiona el caso.
4. **Reembolso (F1).** Cuando un pago pasa de `PAID` a `REFUNDED`, el flujo del reembolso no cambia el estado del pedido. Se avisa a la florista para que gestione el caso. El mecanismo del aviso está pendiente (P11).
5. **Visibilidad del pago en revisión (D5).**
   - El cliente y la florista pueden ver que el pago está en revisión, con mensajes apropiados para cada uno.
   - Si se aplicó C2, el mensaje al cliente aclara que la franja se liberó y que, si el pago se aprueba, la florista coordinará la entrega.
   - El mensaje de "en revisión" solo se usa cuando se aplicó C2. Los demás casos tienen su mensaje en A.23.
6. **`CANCELLED` es un estado final del pedido.**
7. **Compras duplicadas.** Si dos pedidos independientes terminan pagados por una misma compra repetida, la florista lo gestiona a mano. No hay detección ni bloqueo automático.
8. **Varios intentos de pago por pedido (D1-A, aprobada el 2026-11-13).**
   - Un carrito origina un pedido (`Order`). Un pedido puede tener varios pagos (`Payment`), uno por cada intento.
   - Cada intento tiene su propio ciclo de vida y su propia referencia en Mercado Pago.
   - Un intento que termina en `FAILED`, `EXPIRED` o `CANCELLED` se conserva como historial y no se reutiliza: no se le asocia otro cobro ni vuelve a `PENDING`.
     - **Única excepción (A.21, 2026-11-14):** si Mercado Pago confirma después una aprobación de ese intento, pasa a `PAID`, porque el dinero llegó.
   - **Un intento termina cuando la pasarela cierra la unidad de cobro de ese intento** (en Mercado Pago, la order), **no cuando se rechaza una transacción** (2026-11-14). Qué cuenta como cierre lo decide el adaptador de cada pasarela (A.17, A.18).
   - **Los reintentos que ofrece el propio checkout** (por ejemplo, otra tarjeta dentro de la misma order; el checkout solo muestra el medio elegido, así que pasar a otro medio es un cambio de opción, P13) **pertenecen al mismo intento** (2026-11-14). No crean un pago nuevo, no cuentan para el tope de A.9 y no extienden la retención (A.11). Mientras la retención siga vigente, el cliente vuelve a ese mismo checkout con el enlace guardado (A.23). Visto en Mercado Pago (V1); para Wompi, P19.
   - Mientras el pedido siga elegible para pagar, se puede crear otro intento asociado al mismo pedido, sin crear un pedido nuevo. El carrito sigue en el `localStorage` del navegador.
   - Nunca hay dos pagos `PENDING` a la vez para un mismo pedido.
   - Cambiar de opción de pago puede implicar cancelar el intento anterior y crear otro, también sin crear un pedido nuevo. Llega en el PR 2b, con el cierre del cobro anterior verificado antes de crear el nuevo (P13).
9. **Límite de intentos (aprobado el 2026-11-13).**
   - **No se crea un intento nuevo si la retención del pedido ya venció.** El tiempo es el límite principal.
   - **Máximo 5 intentos por pedido.**
     - Solo cuentan los intentos que llegaron a crear el cobro en Mercado Pago (los que tienen referencia). Un intento que falló antes de crear el cobro no cuenta.
     - El tope solo se revisa al crear un intento nuevo, nunca mientras hay uno pendiente. Un intento cuya creación no se confirmó (`FAILED` sin referencia, D.3) no consume el tope. Un intento cerrado por cambiar de opción sí lo consume (P13).
     - El valor 5 es de arranque, sin datos que lo respalden. Se guarda como ajuste en la base de datos (igual que los 15 y 51 minutos), para cambiarlo sin tocar el código, y se ajusta con datos reales.
   - El límite es **por pedido, no por cliente**. Si un pedido agota sus intentos, el cliente puede crear un pedido nuevo con su carrito.
10. **La franja tras un intento sin pago (P2, aprobada el 2026-11-13).**
    - Si un intento falla, vence o se cancela, la franja **sigue reservada** hasta que venza la retención del pedido.
    - El fallo o la cancelación de un intento **no acorta ni reinicia** la retención, y no libera la franja.
    - Los intentos siguientes usan la misma retención del pedido, que nunca pasa del tope de 51 minutos desde su creación.
11. **Extensión de la retención en cada intento (aprobada el 2026-11-13).**
    - Al crear un intento nuevo, el vencimiento de la retención pasa a ser:

      **nuevo vencimiento = el menor entre (tope) y (el mayor entre el vencimiento actual y "ahora + minutos de la opción")**

      donde el tope es la creación del pedido + 51 minutos (`hold_limit_at`).
    - En palabras: se extiende hasta "ahora + los minutos de la opción" si eso es más tarde que el vencimiento actual. Nunca se acorta y nunca pasa del tope.
    - Ejemplo: el vencimiento actual es 10:36 y un intento con una opción de menos minutos calcularía 10:20. El vencimiento se queda en 10:36.
    - La regla es genérica: los minutos son los de cada combinación de pasarela y medio (`payment_method_policy`), no solo los 36 de la tarjeta de Mercado Pago.
    - Es la regla del ADR 0013 (precisión del 2026-11-12), y coincide con el código actual (`OrderHoldService.extendHold`; la base vuelve a comprobar el tope en `updateHold`).
    - No puede revivir una retención vencida, porque con la retención vencida no se crean intentos (A.9). Tampoco ocurre mientras un pago está en revisión, porque ese pago está `PENDING` y no se puede crear otro intento (A.8).
    - **No elimina un caso, resuelto en A.13:** un intento iniciado cerca del tope tiene la retención cortada en el minuto 51, pero su cobro puede seguir abierto en Mercado Pago hasta 15 minutos después de crearse.
12. **El vencimiento del cobro no cancela el pedido (P3, aprobada el 2026-11-13).**
    - Cuando vence el cobro en Mercado Pago (15 minutos), ese intento queda `EXPIRED` (Mercado Pago lo informa como `canceled / expired`, o como `failed / expired` según A.18) y el pedido sigue `PENDING_PAYMENT`, con su franja, hasta que vence la retención (A.10).
    - Mientras la retención siga vigente y no se haya llegado al tope de intentos, el cliente puede crear otro intento (A.8 y A.9).
    - Se acepta que esto permite el caso de A.21 y A.22: una aprobación tardía del intento anterior mientras hay otro abierto.
    - **Lo que cancela un pedido sin pagar es el vencimiento de la retención, no el del cobro.** Al vencer la retención, el job toma el bloqueo del pedido, vuelve a comprobar su estado y `hold_expires_at` (D.9) y, si no hay pago aprobado ni pendiente, lo cancela y libera la franja.
    - Qué hace el job si al vencer la retención todavía hay un pago pendiente está en A.13 (cobro abierto) y en A.2 (C2, `processing / in_process` y `processing / pending_review_manual`); `action_required / waiting_capture` en A.14, y la falta de respuesta en A.15.
13. **El job al vencer la retención con el cobro abierto (P7, caso 1, aprobado el 2026-11-13).**
    - **Qué pedidos busca el job:** todo pedido `PENDING_PAYMENT` con `hold_expires_at` vencido, **tenga o no pagos y sean del estado que sean**. Esta lectura solo encuentra candidatos; no decide nada.
    - **Procesa cada pedido de corrido**, en tres fases:
      1. **Fase 1, sin transacción ni bloqueos:**
         - Si el pedido tiene un pago `PENDING`, consulta su estado a Mercado Pago. Si ese pago no tiene referencia, no consulta nada: se aplica la fila del intento a medio crear (D.3).
         - Si está `created`, pide cancelarlo, siempre con la **misma llave de idempotencia para ese pago** (`X-Idempotency-Key`, obligatoria en ese endpoint).
         - Si la cancelación no responde un 200 con `canceled` (un error, un 409, un tiempo agotado o una respuesta ambigua), no supone nada: **vuelve a consultar el estado real**, y vale lo que diga esa consulta.
         - Si el pedido tiene un pago `PAID`, consulta también ese cobro (ver la fila de reparación).
      2. **Fase 2, en una transacción corta:**
         - Bloquea el pedido y vuelve a comprobar su estado y `hold_expires_at` (D.9).
         - Bloquea el pago, con la primera lectura del pago en esa transacción.
         - Aplica la tabla de abajo.
      3. **Nunca se llama a Mercado Pago con el pedido bloqueado** (D.6).
    - **La ventana entre la fase 1 y la fase 2 se busca mínima, pero no tiene una duración garantizada.** Incluye la espera por una conexión de la base y por el bloqueo del pedido (por ejemplo, si un aviso lo tiene tomado). No se hace una segunda consulta antes de la fase 2: la ventana sería prácticamente igual, y costaría otra llamada y otro posible resultado desconocido. Si Mercado Pago cambia el estado en esa ventana (por ejemplo, aprueba un pago que vimos `in_process`), el desfase queda cubierto por `PAID_WITHOUT_SLOT` y por la siguiente pasada. Se acepta ese desfase.

    **Tabla de decisión de la fase 2** (con el pedido y el pago bloqueados):

    | Lo que se encuentra | Qué hace | Pago | Pedido | Franja |
    |---|---|---|---|---|
    | El pedido ya no está `PENDING_PAYMENT`, o su retención ya no está vencida | Nada (D.9) | — | — | — |
    | Pago `PENDING` **sin referencia** y con el plazo de gracia vencido (intento a medio crear) | Cierra el intento (D.3, P6). En la siguiente pasada, si no queda otro pago pendiente, se cancela el pedido con la fila de "no hay pagos `PENDING` ni `PAID`" | `FAILED`, motivo "creación sin confirmar" | Sin cambios en esta pasada | Sin cambios en esta pasada |
    | Pago `PENDING`; Mercado Pago confirmó `canceled / canceled` | Cierra el intento y cancela el pedido | `CANCELLED` | `CANCELLED` | Se libera |
    | Pago `PENDING`; Mercado Pago confirmó `canceled / expired` | Cierra el intento y cancela el pedido | `EXPIRED` | `CANCELLED` | Se libera |
    | Pago `PENDING`; `failed / expired` **con las condiciones de A.18** | Cierra el intento y cancela el pedido (provisional, V1) | `EXPIRED` | `CANCELLED` | Se libera |
    | Pago `PENDING`; Mercado Pago confirmó `processed / accredited` | Aplica la aprobación (A.1; criterio común de D.10) | `PAID` | `CONFIRMED` | **Se conserva** |
    | Pago `PENDING`; Mercado Pago informó `processing / in_process` o `processing / pending_review_manual` | Aplica C2 (A.2) | Sigue `PENDING` | `PENDING_PAYMENT_NO_SLOT` | Se libera |
    | Pago `PENDING`; `action_required / waiting_retry`, **antes** del vencimiento del cobro más el margen | **Nada.** Espera (A.19). No genera la alerta de A.15 | Sin cambios | Sin cambios | **No se libera** |
    | Pago `PENDING`; `action_required / waiting_retry`, **después** del vencimiento del cobro más el margen | Aplica A.19 según las transacciones | Según A.19 | Según A.19 | Según A.19 |
    | Pago `PENDING`; `failed / failed`, `failed` con otro detalle, o `failed / expired` con todas las transacciones rechazadas pero sin cumplirse el vencimiento según los datos de la order | Aplica A.20 (provisional, V1) | Sigue `PENDING` | `PENDING_PAYMENT_NO_SLOT` | Se libera |
    | Pago `PENDING`; el cobro sigue `created`, la respuesta fue ambigua, el estado o detalle no está reconocido en este ADR, o Mercado Pago no respondió | **Nada.** Se reintenta en la siguiente pasada | Sin cambios | Sin cambios | **No se libera** |
    | No hay pagos `PENDING` ni `PAID` (nunca pagó, o todos los intentos terminaron cerrados, incluso si un aviso los cerró antes) | Cancela el pedido (A.12) | Sin cambios | `CANCELLED` | Se libera |
    | **Pago `PAID` y pedido `PENDING_PAYMENT`, y la consulta de la fase 1 confirmó `processed / accredited`** | **Repara:** completa la confirmación y registra un error operativo, porque se rompió el invariante D.10 | **No se toca** | `CONFIRMED` | **Se conserva** (sigue reservada) |
    | **Pago `PAID` y pedido `PENDING_PAYMENT`, pero Mercado Pago no confirma la aprobación** (otro estado, o no respondió) | **Nada.** Registra un error operativo para revisión manual. No confirma el pedido a ciegas | **No se toca** | Sin cambios | **No se libera** |
    | Pago `PENDING`; Mercado Pago devolvió `action_required / waiting_capture` | **Nada.** Error operativo y alerta (A.14) | Sin cambios | Sin cambios | **No se libera** |

    - **Por qué se verifica antes de reparar:** la reparación supone que `PAID` es una aprobación verificada con Mercado Pago. Si el estado pudo cambiarse a mano sin esa verificación, confirmar el pedido sería un error. La consulta de la fase 1 lo verifica; si no lo confirma, queda para revisión.
    - **Recuperación tras una caída:** si Mercado Pago canceló pero la aplicación se cayó antes de guardar, el pedido sigue `PENDING_PAYMENT` con la retención vencida, y la siguiente pasada lo vuelve a encontrar. Consulta a Mercado Pago, ve `canceled` y aplica la fila que corresponde, sin necesidad de volver a cancelar. Si mientras tanto un aviso cerró el pago, la fila "no hay pagos `PENDING` ni `PAID`" cancela el pedido.
    - **Convergencia:** mientras un pedido esté `PENDING_PAYMENT` con la retención vencida, el job lo vuelve a encontrar en cada pasada. Solo sale de ese grupo al quedar `CONFIRMED`, `CANCELLED` o `PENDING_PAYMENT_NO_SLOT`, y ninguna de esas salidas libera la franja dos veces. Las excepciones intencionales son los resultados desconocidos, que no liberan y a los 15 minutos de `hold_expires_at` generan una alerta (A.15), y la espera de `waiting_retry`, que tiene su propio plazo (A.19).
14. **`action_required / waiting_capture`: estado no soportado (P7, caso 3, aprobado el 2026-11-13).**
    - **Nuestra configuración:** usamos Checkout Pro mediante la API de Orders. Checkout Pro exige `processing_mode=manual`, pero eso **no equivale** a `capture_mode=manual`.
      - La guía "Crear y configurar una order de pago" de Checkout Pro (consultada el 2026-11-13) muestra una solicitud que omite `capture_mode` y cuya respuesta devuelve `automatic_async`. Para Checkout Pro solo menciona `automatic` (resultado binario) y `automatic_async` (flujos asíncronos).
      - La referencia de la API, "Crear order" (consultada el 2026-11-13), define `manual` como el modo que reserva el monto en la tarjeta para capturarlo después con `POST /v1/orders/{order_id}/capture`.
      - Nuestra integración no envía `capture_mode`, y nuestras orders de prueba devuelven `automatic_async` (V5). Por eso no esperamos ver `waiting_capture`.
    - **No se implementa la captura manual.**
    - **Si la consulta a Mercado Pago devuelve `action_required / waiting_capture`:**
      - no se modifica el pago ni el pedido;
      - no se libera la franja;
      - se registra un error operativo para revisión;
      - se emite una **alerta operativa**, porque la autorización puede tener fondos reservados en la tarjeta del cliente.
    - El mecanismo concreto de la alerta está pendiente (P11).
    - **Comprobación V5, hecha el 2026-11-13:** nuestras orders tienen `capture_mode: automatic_async`.
15. **Mercado Pago no responde al vencer la retención (P7, caso 4, aprobado el 2026-11-13).**
    - Mientras Mercado Pago no responda y no se pueda conocer el estado real del cobro, **no se libera la franja ni se modifican los estados internos** (A.13).
    - El job sigue reintentando la consulta en cada pasada.
    - Si el pedido sigue sin resolverse cuando han pasado **15 minutos desde `hold_expires_at`**, se genera una **alerta operativa**.
      - El tiempo se cuenta desde `hold_expires_at`, que está guardado en el pedido. Así, reiniciar el job o la aplicación no reinicia la espera.
      - Los 15 minutos son un ajuste en la base de datos, con 15 como valor inicial, para cambiarlo sin tocar el código. Es un valor de arranque, sin datos que lo respalden.
      - La alerta no se repite en cada pasada: un mismo pedido no genera notificaciones duplicadas.
    - **La alerta no libera la franja.** La regla sigue siendo no liberar mientras el resultado sea desconocido. La alerta solo pide que una persona lo revise.
    - **Por qué no se libera:** si Mercado Pago está caído para todos, ningún otro cliente puede pagar y retener la franja casi no le quita nada a nadie; si solo falla la consulta de ese cobro, el problema necesita una persona, no una regla automática.
    - El mecanismo concreto de la alerta, y cómo se evita duplicarla, está pendiente (P11).
    - **No aplica mientras dura la espera de A.19** (`waiting_retry`), que tiene su propio plazo y su propia alerta.
16. **Pedido sin franja cuyo pago termina sin aprobarse (P8, aprobado el 2026-11-13).**
    - Si el pedido está `PENDING_PAYMENT_NO_SLOT` y el pago que quedó pendiente (tras C2, A.19 o A.20) termina en un estado definitivo sin aprobación, **el pedido pasa a `CANCELLED`**.
    - El pago pasa al estado interno que corresponda al resultado definitivo **reconocido** de Mercado Pago:
      - `canceled / canceled` → `CANCELLED`;
      - `canceled / expired` → `EXPIRED`;
      - `failed / expired`, **con las condiciones de A.18** → `EXPIRED` (regla operativa provisional, 2026-11-14).
    - **`failed / failed`, y `failed` con otro detalle, no cancelan el pedido** mientras V1 siga abierta (A.20). El pedido sigue `PENDING_PAYMENT_NO_SLOT` y la conciliación lo sigue revisando (A.17).
    - **Solo se cancela con un estado definitivo verificado, o adoptado de forma explícita como regla operativa provisional** e identificado como tal en este documento. Hoy son los dos `canceled` de arriba (V2 y la prueba de vencimiento en el sandbox) y, como regla provisional, `failed / expired` (A.18). La lista no se amplía sin una de esas dos condiciones.
    - **La franja no se toca:** ya se liberó al pasar el pedido a `PENDING_PAYMENT_NO_SLOT`.
    - **No se permiten intentos nuevos** sobre ese pedido, porque su retención ya venció (A.9). Si el cliente quiere comprar igual, crea un pedido nuevo con el carrito que conserva el navegador.
    - **Quién lo resuelve:** el aviso de Mercado Pago puede iniciar la resolución, pero siempre se consulta el estado real a Mercado Pago (D.2) y se actualiza con el pedido bloqueado, en el orden pedido → pago (D.7). El job de A.13 **no** cubre este caso, porque solo busca pedidos `PENDING_PAYMENT`. También lo revisa la conciliación de A.17.
    - La notificación al cliente está pendiente (P11).
17. **Conciliación de pagos sin resolver (P18, aprobada el 2026-11-14).**
    - **Regla general, independiente de la pasarela.** Un proceso de conciliación revisa periódicamente, por si el aviso de la pasarela no llega o no se pudo procesar:
      - los pedidos `PENDING_PAYMENT_NO_SLOT`, para resolver el pago que quedó pendiente (A.2, A.19 y A.20);
      - los pagos `PENDING` de pedidos que ya no están `PENDING_PAYMENT` ni `PENDING_PAYMENT_NO_SLOT`, como el segundo intento que quedó abierto tras una aprobación tardía (A.21).
    - **Secuencia, para cada caso:**
      1. Consulta a la pasarela el estado real del pago, fuera de toda transacción y sin bloqueos (D.6).
      2. Abre una transacción.
      3. Bloquea el pedido y después el pago (D.7).
      4. Comprueba que el pedido y el pago siguen en el estado que motivó la revisión.
      5. Solo entonces aplica el resultado. Si un aviso ya lo resolvió, no sobrescribe nada.
    - **Qué aplica:**
      - **Aprobación verificada:** según el criterio común de D.10, A.21 y A.22, en la misma transacción que el pedido.
      - **El adaptador confirma que el intento terminó sin aprobación:** con el pedido `PENDING_PAYMENT_NO_SLOT`, se aplica A.16; con otro estado del pedido, solo se cierra el pago.
      - **Cualquier otro caso** (sin resolver, no reconocido, sin confirmar o sin respuesta): no cambia nada.
    - **Una transacción rechazada no es un intento cerrado.** Que una transacción termine rechazada no basta para cerrar nuestro pago. Ejemplos: la order de Mercado Pago en `action_required / waiting_retry`, que después se aprobó con una segunda transacción (V1), y el reintento de Wompi, que crea otra transacción con la misma referencia (P19).
    - **No libera franjas ni crea intentos.**
    - **Frecuencia:** cada hora. Es un ajuste en la base, con 1 hora de arranque.
    - **Alerta:** si un pedido sigue `PENDING_PAYMENT_NO_SLOT` sin resolverse 24 horas después del instante en que entró en ese estado (F), se genera una alerta interna.
      - Las 24 horas son un ajuste en la base, con 24 de arranque.
      - La alerta no cambia estados ni se repite en cada pasada. Su mecanismo está pendiente (P11).
      - *Recomendación sin aprobar:* los pedidos que llegaron por A.20 van en una revisión agrupada, no en una alerta individual.
    - **Alcance de D.2, D.3 y D.6:** en esta conciliación valen para cualquier pasarela.
    - **Por qué hace falta:** ninguna de nuestras integraciones garantiza un plazo de resolución, y los avisos pueden perderse. Mercado Pago no publica un plazo para Checkout Pro vía Orders ("menos de 2 días hábiles" es de Payments API). Wompi reintenta un aviso como máximo 3 veces en 24 horas.
    - **Límite aceptado:** no consulta los intentos ya cerrados. Una aprobación tardía de un intento cerrado solo se detecta si llega un aviso (A.21). Lo mitiga P20.
    - **Lo que queda en cada adaptador:** la traducción de estados, qué cierra un intento, qué se puede cancelar según el medio de pago y los reintentos propios de la pasarela (P19).
18. **`failed / expired`: el cobro venció con transacciones rechazadas (regla operativa provisional, aprobada el 2026-11-14).**
    - Se trata como vencimiento del cobro **solo si se cumplen las tres condiciones**:
      1. Mercado Pago informa estado `failed` y detalle `expired`.
      2. Con los datos de la propia order (`created_date` más `expiration_time`), el vencimiento ya pasó.
      3. Ninguna transacción está aprobada, en proceso o con un valor no reconocido: todas son `failed`, o no hay ninguna.
    - **Efecto:** el pago pasa a `EXPIRED`, igual que con `canceled / expired`.
      - Con la retención vigente, el pedido sigue `PENDING_PAYMENT` y se puede crear otro intento (A.12).
      - Al vencer la retención, el pedido se cancela (A.13).
      - Con el pedido `PENDING_PAYMENT_NO_SLOT`, se cancela (A.16).
    - **Si falta alguna condición:**
      - si una transacción está aprobada, en proceso o con un valor no reconocido, es un **caso no reconocido**: no se libera la franja y hay alerta;
      - si todas las transacciones están rechazadas pero el vencimiento no se cumple según los datos de la order, se aplica A.20.
    - **Por qué es provisional:** se basa en lo visto en el sandbox (V1, 2026-11-14), no en la documentación de Checkout Pro, que no incluye ese detalle. Si Mercado Pago aprueba después un intento cerrado así, aplica A.21.
19. **`action_required / waiting_retry` (aprobada el 2026-11-14).**
    - **Mientras la retención siga vigente:** el pago sigue `PENDING` y la franja sigue reservada; no se crea otro intento (A.8); el cliente puede volver al mismo checkout (A.8 y A.23).
    - **Cuando vence la retención, el job espera** sin liberar la franja hasta que pase el vencimiento del cobro más un margen.
      - El margen es un ajuste en la base, con 15 minutos de arranque.
      - Lo normal es que, para entonces, la order ya esté `failed / expired` (A.18).
      - **La espera no depende de cancelar la order.** Pedir la cancelación es opcional, y su resultado no se usa como garantía (V6).
    - **Pasado el margen, si sigue en `waiting_retry`,** el job vuelve a consultar y decide según las transacciones:

      | Lo que encuentra | Pago | Pedido | Franja |
      |---|---|---|---|
      | La order pasó a `processed / accredited` | `PAID` | Según el criterio común de D.10 | Según D.10 |
      | Alguna transacción `processing` | Sigue `PENDING` | `PENDING_PAYMENT_NO_SLOT` (mismo efecto que C2, con motivo propio) | Se libera |
      | Todas las transacciones `failed` | Sigue `PENDING` | `PENDING_PAYMENT_NO_SLOT`, con el motivo "cobro abierto después de su vencimiento" | Se libera |
      | Una transacción aprobada con la order sin `processed`, un valor de transacción no reconocido, una respuesta ambigua o ninguna respuesta | Sin cambios | Sin cambios | **No se libera** |

    - **Valores de transacción reconocidos:** `failed` (rechazada), `processed` (aprobada) y `processing` (en proceso). Cualquier otro es no reconocido.
    - **Qué cuenta como aprobación:** la **order** en `processed / accredited`, nunca una transacción por sí sola (B y D.10).
    - En los tres casos de las filas 2 a 4 se genera una **alerta operativa**: que una order siga abierta después de su vencimiento no es lo esperado.
    - Una aprobación posterior deja el pedido en `PAID_WITHOUT_SLOT` (A.3).
    - **Supone un vencimiento del cobro corto** (F).
20. **`failed` sin cierre confirmado (regla operativa provisional, aprobada el 2026-11-14).**
    - Comprende `failed / failed`, `failed` con cualquier detalle que no sea `expired`, y `failed / expired` con todas las transacciones rechazadas pero sin cumplirse el vencimiento según los datos de la order (A.18).
    - **Mientras la retención siga vigente:** el pago sigue `PENDING`, no se crea otro intento y la franja sigue reservada.
    - **Al vencer la retención:** la franja se libera, el pedido pasa a `PENDING_PAYMENT_NO_SLOT` y el pago sigue `PENDING`, con el motivo "rechazado, sin cierre confirmado". No se permite otro cobro.
    - **No cancela el pedido** mientras V1 siga abierta (A.16). Lo revisa la conciliación (A.17).
    - Una aprobación posterior deja el pedido en `PAID_WITHOUT_SLOT` (A.3).
    - **No es C2:** tiene otro motivo y otro mensaje (A.23).
    - **Cuándo se revisa:** cuando se resuelva V1. Si `failed` resulta terminal, estos pedidos pasan a `CANCELLED` y esta regla se sustituye.
21. **Aprobación verificada de un intento cerrado (aprobada el 2026-11-14).**
    - Si Mercado Pago confirma `processed / accredited` (D.2) para un pago `EXPIRED`, `CANCELLED` o `FAILED`, el pago pasa a `PAID`. Es la excepción a A.8: el dinero llegó (ADR 0012, punto 10).
      - `FAILED → PAID` solo es posible si el pago tiene la referencia de Mercado Pago. Sin ella no se puede verificar (P6, P16).
    - Se hace con el pedido bloqueado y después el pago (D.7), con una actualización condicional sobre el estado cerrado esperado (D.8).
    - **El pedido sigue el criterio común de D.10**, igual que cualquier otra aprobación:

      | Estado del pedido al bloquearlo | Pago | Pedido | Franja | Además |
      |---|---|---|---|---|
      | `PENDING_PAYMENT`, con la franja asignada | `PAID` | `CONFIRMED` | Se conserva | Después de guardar, se intenta cancelar el otro intento abierto, sin depender del resultado |
      | `PENDING_PAYMENT_NO_SLOT` | `PAID` | `PAID_WITHOUT_SLOT` (A.3) | — | Igual que la fila anterior, si hay otro intento abierto |
      | `CANCELLED` | `PAID` | Sigue `CANCELLED` (A.6) | — | Error operativo y alerta. La florista decide si entrega o devuelve |
      | `CONFIRMED` o `PAID_WITHOUT_SLOT` | `PAID` | Sin cambios | — | Pago duplicado (A.22) |

    - **El vencimiento de la retención no interviene.** Con la retención vencida y la franja todavía asignada, se confirma (D.10, P15).
    - **El otro intento que siga `PENDING`** se resuelve por su cuenta, con el aviso o con la conciliación (A.17). Si se aprueba, aplica A.22. No se crea ningún pago nuevo.
    - **Límite aceptado:** solo se detecta si llega el aviso o una consulta (A.17).
22. **Pagos duplicados (aprobada el 2026-11-14).**
    - **Dos pagos internos aprobados en un mismo pedido.** Si un pedido ya `CONFIRMED` o `PAID_WITHOUT_SLOT` recibe otra aprobación verificada:
      - ese pago se registra como `PAID`;
      - el pedido no cambia;
      - se registra un error operativo y una alerta.

      La florista gestiona la devolución. **No hay devolución automática** (A.4 y A.7). Los duplicados entre pedidos distintos siguen como en A.7.
    - **Varias transacciones aprobadas dentro de una misma order de Mercado Pago.** Es otra anomalía. No se ha observado, y no se supone que Mercado Pago la permita ni que la impida.
      - **Nuestro pago se registra una sola vez:** pasa a `PAID` cuando la **order** está `processed / accredited`.
      - Nunca se cuentan transacciones ni se suman sus montos. El importe es el de nuestro pago (el del pedido).
      - Al registrar la aprobación, el adaptador revisa las transacciones. Si hay más de una aprobada, o la suma de las aprobadas supera el total de la order: el pago queda `PAID` igual, se registra un error operativo y una alerta, y la florista lo resuelve con la pasarela.
      - La pregunta está incluida en P20.
23. **Mensajes al cliente según el motivo (criterio aprobado el 2026-11-14; los textos son orientativos).**
    - La página y el correo se basan en nuestro estado y su motivo, nunca en lo que trae la redirección desde la pasarela (D.2).
    - **Nunca se muestra "en revisión" si sabemos que la transacción fue rechazada.**
    - Se escribe "no se aprobó ningún pago", no "no se te cobró": algunos bancos muestran una retención temporal aunque el pago se rechace.

    | Situación | Mensaje orientativo |
    |---|---|
    | Aprobado (`CONFIRMED`) | "¡Listo! Tu pago se aprobó y tu pedido está confirmado para [franja]." |
    | Rechazo con la retención vigente (`waiting_retry`) | "Tu pago fue rechazado. Puedes intentarlo de nuevo, por ejemplo con otra tarjeta, antes de las HH:MM." Botón **Volver a pagar** al mismo checkout. HH:MM es el vencimiento del **cobro** (F), no el de la retención: después, el enlace ya no sirve |
    | El cobro venció con la retención vigente (intento `EXPIRED`) | "El enlace de pago venció. Puedes intentarlo de nuevo antes de las HH:MM." |
    | Pedido cancelado sin pago (`canceled`, `canceled / expired` o `failed / expired`) | "No se aprobó ningún pago y liberamos el horario que tenías reservado. Elige otro y te guardamos todo lo demás. Si ves un cobro aprobado en tu banco, escríbenos y lo revisamos." Botón **Elegir otro horario** (precisión del 2026-11-20, abajo) |
    | C2 (`processing`), y A.19 con una transacción en proceso | "Tu pago está en revisión. Liberamos tu horario; si se aprueba, la florista te contactará para coordinar la entrega." |
    | A.20 (`failed` sin cierre confirmado) y A.19 con todas las transacciones rechazadas | "Los intentos de pago registrados fueron rechazados y el tiempo de reserva terminó, por lo que liberamos tu horario. La plataforma de pago aún no ha confirmado el cierre de tu intento de pago anterior. Antes de realizar otro pago, comunícate con nosotros. Si ves una aprobación, avísanos para revisarla." |
    | Error al iniciar el pago (`FAILED`, sin referencia) | "No pudimos iniciar el pago. Intenta de nuevo." |
    | El pedido llegó al tope de intentos (A.9) | "Llegaste al máximo de intentos de pago para este pedido. Si quieres, haz un pedido nuevo." |
    | Pago pendiente de confirmación (el pago sigue `PENDING` y todavía no se aplica ninguna regla de la tabla: por ejemplo, el cliente vuelve del checkout antes de que llegue el aviso) | "Estamos confirmando el estado de tu pago. Te avisaremos por correo." |
    | `PAID_WITHOUT_SLOT` | "Recibimos tu pago. La florista te contactará para coordinar la entrega." |
    | Aprobación sobre un pedido `CANCELLED`, o pago duplicado | "Recibimos un pago adicional. La florista te contactará para resolverlo." |

    - En A.19 y A.20 **no se invita a crear otro pedido**, porque el intento anterior todavía podría admitir un pago. Sí se invita en A.18, porque se comprobó que el cobro venció.
    - **"Elegir otro horario" (precisión del 2026-11-20).** Cuando el pedido se cancela sin pago, el cliente lo vive como elegir otra hora, no como empezar de cero. El botón lo lleva al último paso con el carrito intacto y con lo que ya escribió (quién recibe, mensaje de la tarjeta, nota) precargado; solo elige día y hora y paga. **Por dentro es un pedido nuevo**: el cancelado no se reabre, porque `CANCELLED` es final (sección C) y reabrirlo obligaría a cambiar las reglas de estados, el job y el tope de intentos, con el riesgo de que un pago viejo se apruebe tarde y se confunda con el nuevo. Los datos para precargar los devuelve la consulta del pedido (pendiente 34).
    - Los mensajes a la florista corresponden a las alertas de A.14, A.15, A.17, A.19, A.21 y A.22. El mecanismo de envío está pendiente (P11).

### B. Máquina de estados del pago

**Estados internos:** `PENDING`, `PAID`, `FAILED`, `EXPIRED`, `CANCELLED` (ADR 0003) y `REFUNDED` (nuevo, por la decisión A.4).

| Origen → destino | Qué la provoca | Condiciones | ¿Reversible? | Situación |
|---|---|---|---|---|
| (nuevo) → `PENDING` | Iniciar un pago | El pedido está `PENDING_PAYMENT`, su retención no venció, no tiene otro pago `PENDING` (ADR 0014, punto 3) y no llegó al tope de intentos (A.8 y A.9) | — | Aprobada |
| `PENDING → PAID` | Mercado Pago confirma la aprobación al consultarle (`processed / accredited`) | El pago está `PENDING`; el estado se consulta a la pasarela, nunca se toma del aviso | No (solo puede pasar a `REFUNDED`) | Aprobada |
| `PENDING → EXPIRED` | Mercado Pago confirma que ya no se puede pagar: `canceled / expired`, o `failed / expired` con las condiciones de A.18 | El pago está `PENDING` (ADR 0012, punto 9) | Solo a `PAID` (A.21) | Aprobada; `failed / expired` es provisional (A.18) |
| `PENDING → FAILED` | Falla al crear el cobro en la pasarela (`markFailed`), incluida la creación sin confirmar y el intento a medio crear (D.3). **`failed / failed` de Mercado Pago no lleva aquí** (A.20) | El pago está `PENDING` (actualización condicional, D.8) | Solo a `PAID`, si tiene referencia (A.21) | Aprobada (P5 y P6). El motivo se guarda aparte: "rechazada por la pasarela" o "creación sin confirmar" |
| `PENDING → CANCELLED` | Mercado Pago confirma `canceled / canceled` (por ejemplo, tras la cancelación del job en A.13 o del cambio de opción en P13) | El pago está `PENDING` (actualización condicional, D.8) | Solo a `PAID` (A.21) | Aprobada (A.13, A.16, D.4 y P13). Un `canceled` con otro detalle es no reconocido (P14) |
| `EXPIRED`, `CANCELLED` o `FAILED` → `PAID` | Mercado Pago confirma `processed / accredited` para un intento cerrado | Consulta verificada (D.2); pedido y pago bloqueados (D.7); actualización condicional (D.8); `FAILED` solo con referencia | No (solo a `REFUNDED`) | **Aprobada (A.21), excepción a A.8** |
| `PAID → REFUNDED` | Mercado Pago informa un reembolso total (`refunded`) | El pago está `PAID` | No | Aprobada (A.4) |

- **Ningún otro cambio es válido.** Un estado final no cambia, salvo `PAID → REFUNDED` y la aprobación verificada de un intento cerrado (A.21). Pasar al mismo estado es un error.
- **"En revisión" no es un estado interno.** Es un pago `PENDING` cuyo estado del proveedor dice revisión (A.5 y regla D.1).
- **Un pago `PENDING` puede tener distintos motivos:** en revisión (C2), en espera de reintento o cobro abierto después de su vencimiento (A.19), o rechazado sin cierre confirmado (A.20). El motivo **no es un estado interno**: se guarda aparte (D.1 y F).
- **No hay estado `UNKNOWN`** (P6, D.3), **ni dos estados de falla** (P5): un solo `FAILED`, con su motivo aparte.

### C. Máquina de estados del pedido

**Estados:** `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `PAID_WITHOUT_SLOT` (ADR 0012) y `PENDING_PAYMENT_NO_SLOT` (nuevo, por la decisión A.2).

| Origen → destino | Qué la provoca | Condiciones | Franja | ¿Reversible? | Situación |
|---|---|---|---|---|---|
| (nuevo) → `PENDING_PAYMENT` | Crear el pedido | Hay cupo en la franja | Se reserva | — | Aprobada (ya existe) |
| `PENDING_PAYMENT → CONFIRMED` | Un pago pasa a `PAID`, desde `PENDING` o desde un intento cerrado (A.21) | El pedido está `PENDING_PAYMENT` y tiene la franja asignada, con el bloqueo puesto (D.10). Vale también con `hold_expires_at` vencido, mientras la franja no se haya liberado | Se conserva | No automáticamente | Aprobada (A.1, D.10). P15 resuelta |
| `PENDING_PAYMENT → PENDING_PAYMENT_NO_SLOT` | El job, al vencer la retención: C2 (A.2), `waiting_retry` pasado el margen (A.19) o `failed` sin cierre confirmado (A.20) | El pedido sigue `PENDING_PAYMENT` y la retención sigue vencida, revisados con el bloqueo puesto (D.9) | Se libera | No | A.2 y A.19 aprobadas; A.20 provisional. Otros estados del proveedor: A.13 a A.15 |
| `PENDING_PAYMENT_NO_SLOT → PAID_WITHOUT_SLOT` | El pago pasa a `PAID` | El pedido está `PENDING_PAYMENT_NO_SLOT` | Ninguno (ya estaba libre) | No automáticamente | Aprobada (A.2 y A.3) |
| `PENDING_PAYMENT → CANCELLED` | El job: vence la retención sin pago aprobado ni pendiente (A.12), incluido el cierre por `failed / expired` (A.18) | El pedido sigue `PENDING_PAYMENT` y su retención sigue vencida al revisarlos con el bloqueo puesto (D.9) | Se libera | No (A.6) | Aprobada. Un intento sin pago no la provoca (A.10 y A.12). Con un pago todavía pendiente: A.13 a A.15 |
| `PENDING_PAYMENT_NO_SLOT → CANCELLED` | El pago pendiente termina en un estado definitivo reconocido, o en uno adoptado como regla provisional, sin aprobación (A.16, A.18) | El pedido está `PENDING_PAYMENT_NO_SLOT`; consulta real a Mercado Pago; pedido bloqueado | Ninguno (ya estaba libre) | No (A.6) | Aprobada (A.16). La revisan el aviso y la conciliación (A.17) |
| `PAID_WITHOUT_SLOT → ?` | Gestión de la florista | — | — | — | **Pendiente: P10** |
| `CONFIRMED → ?` | — | — | — | — | Este ADR no define salidas. El reembolso no lo cambia (A.4) |
| `CANCELLED → (ninguno)` | — | — | — | — | Aprobada (A.6). Un pago puede pasar a `PAID` con el pedido `CANCELLED` (A.21); el pedido no cambia |

### D. Reglas de arquitectura (aprobadas como concepto; su cumplimiento se verifica al implementar)

1. **El estado del proveedor se guarda aparte del estado interno.** El pago guarda el último estado informado por Mercado Pago en un campo separado. No se crea un estado interno por cada estado del proveedor. La traducción entre los dos no es uno a uno; la tabla completa está pendiente (P14).
2. **El estado siempre se consulta a la pasarela.** Nunca se toma del cuerpo del aviso (ADR 0003 y 0014, punto 5). Un aviso solo dice "consulta este cobro".
3. **No recibir respuesta no es un fallo.** Si Mercado Pago no responde (por ejemplo, por un tiempo agotado), no se puede suponer que el pago falló: el proveedor pudo haberlo creado o procesado.
   - **Al consultar o cancelar un cobro:** no se cambia ningún estado y se reintenta después (A.13, A.15 y A.17).
   - **Al crear el cobro (P6, aprobada el 2026-11-14):** el intento se cierra como `FAILED`, con el motivo "creación sin confirmar". No se afirma que Mercado Pago no lo creara: se cierra **nuestro intento** porque nadie puede pagarlo. La URL de pago solo llega al cliente después de guardarla (`attachCheckout`), y solo se obtiene con nuestra credencial y el id de la order, que no recibimos. Si Mercado Pago lo creó, esa order no tiene quien la pague y vence sola.
   - **Intento a medio crear:** un pago `PENDING` **sin referencia** que lleva más que un plazo de gracia (por ejemplo, porque la aplicación se cayó entre crear el cobro y guardarlo) se cierra igual, como `FAILED` con el motivo "creación sin confirmar", con una actualización condicional (D.8).
     - Lo cierran el inicio de un pago nuevo, para que el cliente no quede bloqueado, y el job (A.13).
     - El plazo de gracia es de 1 minuto. Tiene que ser mayor que la espera máxima con la pasarela (hoy 5 s de conexión y 15 s de lectura), así que va en la configuración, junto a esos tiempos, y no en la base de datos.
   - **Por qué vale, y cuándo no (precisado el 2026-11-15):**
     - Esta regla aplica porque crear una order en Checkout Pro no realiza el cobro. La creación únicamente genera la order y su `checkout_url`. El dinero se mueve cuando el comprador accede al checkout y completa el pago.
     - Si la respuesta de creación se pierde antes de entregar la `checkout_url` al comprador, y esta no queda expuesta por otro medio, la order creada no tiene una vía de pago accesible para él. El intento interno puede cerrarse como `FAILED`, con motivo "creación sin confirmar", y permitir un nuevo intento. La order anterior queda sujeta a su vencimiento configurado.
     - Esta regla no aplica a integraciones que cobran al crear la operación. Por ejemplo, una operación directa con tarjeta mediante Checkout API o una transacción directa de Wompi. En esos escenarios, una creación sin confirmar debe conciliarse con la llave de idempotencia o la referencia antes de crear otro intento. La integración correspondiente necesitará su propia decisión.
     - **Para que esta regla siga valiendo, la `checkout_url` solo se entrega al comprador después de guardarla con el intento (`attachCheckout`), y no se escribe en logs ni en mensajes de error.** Si un cambio futuro la expusiera antes, esta regla deja de aplicar y habría que conciliar antes de reintentar.
   - **No existe un estado `UNKNOWN`.** La máquina de estados y el índice de un solo pago `PENDING` (migración V8) no cambian.
   - Esos intentos no cuentan para el tope de A.9, porque no tienen referencia.
   - Si Mercado Pago creó la order "fantasma", sus avisos llegan con un id que no conocemos (P16). Nunca crean un pago.
4. **Avisos idempotentes: la regla central del aviso (P4, aprobada el 2026-11-15).** Los avisos de la pasarela pueden repetirse o llegar desordenados. Procesar un aviso repetido no repite sus efectos: ni el cambio de estado, ni la liberación de la franja, ni el aviso a la florista o al cliente. La misma regla la usa la conciliación (A.17).
   - **Secuencia:**
     1. Validar la firma del aviso (ADR 0003 y 0014). **Si la firma es inválida o falta** (P16, parte 3, aprobada el 2026-11-15): se responde **401**, no se consulta a la pasarela, no se procesa nada y se registra en el log.
        - Así, si el aviso era real y nuestra clave estaba mal configurada, la pasarela lo sigue reenviando, y al corregir la clave se procesa solo. Responder 200 haría perder avisos reales sin enterarnos.
        - Si se repiten muchos avisos con firma inválida seguidos, se genera una alerta, porque probablemente la clave está mal configurada (P11).
        - Sin clave configurada en el servidor, se rechazan todos los avisos y se registra un error al arrancar. Nada se acepta sin verificar.
        - **Por ahora no se limita la antigüedad del aviso** (el `ts` de la firma). No se sabe si la pasarela lo renueva en cada reintento, y un límite podría rechazar reintentos legítimos. Un aviso repetido no hace daño (D.4). Se revisa con V8.
        - Aunque un aviso falso pasara, no podría cambiar el estado de un pago, porque el estado siempre se consulta a la pasarela (D.2). La firma protege sobre todo de que alguien nos obligue a hacer consultas sin fin.
     2. Buscar nuestro pago por la pasarela y la referencia del cobro. **Si la referencia es desconocida** (P16, parte 2, aprobada el 2026-11-15): se responde 200, se registra en el log con su id, **no se consulta a la pasarela y nunca se crea un pago**.
        - Casos: una order "fantasma" de una creación sin confirmar (D.3); un aviso de order creada que llega antes de guardar la referencia (`attachCheckout`), cuando el cliente todavía no tiene el enlace; o una order ajena a la tienda en la misma cuenta.
        - En ninguno hay dinero en juego: nadie puede pagar una order cuyo enlace no entregamos. Responder con un error solo haría que la pasarela lo reenviara sin final, sin un pago que actualizar.
        - Vale por la misma razón que D.3: crear el cobro no cobra. Si una pasarela cobra al crear la operación, esta regla se revisa con su propia decisión.
     3. Consultar el estado real a la pasarela, fuera de toda transacción (D.2 y D.6). Si no responde, no se cambia nada y se contesta con un error para que la pasarela reintente (D.3).
     4. El adaptador traduce lo consultado a uno de cinco resultados, siempre con el par exacto de estado y detalle:
        - **aprobado:** `processed / accredited`;
        - **cerrado:** `canceled / canceled`, `canceled / expired`, o `failed / expired` con las condiciones de A.18;
        - **reembolsado:** `refunded / refunded`;
        - **sin cambio:** `created / created`, `processing / in_process`, `processing / pending_review_manual` (A.2), `action_required / waiting_retry` (A.19), o `failed` sin cierre confirmado (A.20);
        - **no reconocido:** cualquier otro par, incluidos `action_required / waiting_capture` (A.14), un `canceled` o un `processing` con otro detalle (A.2, P14) y `processed / partially_refunded` (P14).
     5. En una transacción, bloquear el pedido y después el pago (D.7), releer los dos y aplicar la tabla de abajo, con actualizaciones condicionales (D.8).
     6. Los efectos hacia afuera (avisos al cliente o a la florista) solo salen si **en esta transacción** hubo un cambio de estado. Mientras no exista su mecanismo (P11), se registran en el log.
     7. Contestar 200 después de guardar; 503 si la transacción falló o se llegó a un tiempo máximo (ver "Cómo se procesa y en cuánto tiempo", más abajo).

   - **Tabla de decisión:**

     | Nuestro pago | Resultado | Qué hace |
     |---|---|---|
     | `PENDING` | Aprobado | `PAID`; el pedido según el criterio común de D.10; revisión de varias transacciones aprobadas en la order (A.22) |
     | `PENDING` | Cerrado | `EXPIRED` o `CANCELLED`, según el resultado. El pedido: si está `PENDING_PAYMENT_NO_SLOT`, pasa a `CANCELLED` (A.16); si está `PENDING_PAYMENT`, no cambia y lo decide el job al vencer la retención (A.12); si ya está `CONFIRMED` o `PAID_WITHOUT_SLOT` (un segundo intento tras A.21), solo se cierra el pago |
     | `PENDING` | Sin cambio | Solo se actualiza el estado del proveedor (D.1) |
     | `PENDING` | Reembolsado | No cambia nada; error operativo y alerta. Provisional hasta resolver P9 |
     | `PENDING` | No reconocido | No cambia nada; error operativo y alerta (A.14) |
     | `EXPIRED`, `CANCELLED` o `FAILED` con referencia | Aprobado | A.21 |
     | `EXPIRED`, `CANCELLED` o `FAILED` | Cualquier otro | Nada |
     | `PAID` | Reembolsado | `REFUNDED`; el pedido no cambia (A.4) |
     | `PAID` | Aprobado | Nada: es un aviso repetido |
     | `PAID` | Sin cambio o no reconocido (por ejemplo, `processed / partially_refunded` o un contracargo) | No cambia nada; error operativo y alerta si es no reconocido (P14) |
     | `PAID` | Cerrado | No cambia nada; error operativo y alerta (una order aprobada no debería pasar a cerrada) |
     | `REFUNDED` | Cualquiera | Nada |

   - **El aviso nunca libera una franja.** Liberar es solo del job (A.13 y D.9). Así, un aviso repetido no puede liberarla dos veces.
   - **Los avisos desordenados o viejos no afectan,** porque siempre se usa el estado actual que devuelve la consulta.
   - **Las alertas que no vienen de un cambio de estado no se duplican:** un mismo pago con el mismo motivo genera una sola alerta, aunque lleguen varios avisos. El mecanismo está en P11.
   - **Reemplaza el ADR 0014, punto 5** ("si el pago ya no está pendiente, el aviso se ignora"): no se ignoran los reembolsos ni las aprobaciones de intentos cerrados.
   - **Cómo se procesa y en cuánto tiempo (P16, parte 1, aprobada el 2026-11-15).**
     - **Lo que exige Mercado Pago** (página de avisos de Orders, consultada el 2026-11-15): responder 200 o 201 en un máximo de 22 segundos. Si no respondemos a tiempo, o respondemos con un error, reintenta cada 15 minutos; después del tercer intento espacia los envíos, pero sigue enviando. No documenta cuándo se detiene.
     - **Se procesa dentro de esos 22 segundos (opción A).** Mercado Pago actúa como nuestra cola de reintentos: si algo falla, respondemos con un error y el aviso vuelve a llegar.
     - **Tiempos máximos de cada paso**, para que la suma quede por debajo de 22 segundos:

       | Paso | Tiempo máximo |
       |---|---|
       | Conectar con la pasarela para consultar | 3 s |
       | Respuesta de la pasarela a la consulta | 8 s |
       | Conseguir una conexión a la base | 3 s |
       | Esperar el bloqueo del pedido o del pago | 5 s |
       | **Total en el peor caso** | **unos 19 s** |

       - Son valores técnicos: van en la configuración, no en la base de datos, y se ajustan con lo que se vea en producción.
       - La espera al **crear** el cobro (5 s de conexión y 15 s de respuesta) no cambia: ahí espera el cliente, no la pasarela.
       - Si se llega a cualquiera de estos límites, se responde **503** y no se cambia nada. La pasarela reintenta, y si el aviso no llega nunca, el job y la conciliación terminan resolviendo el pago (A.13 y A.17).
     - **Requisito de diseño: la recepción del aviso va separada de su procesamiento.**
       - **Recepción** (adaptador de entrada HTTP): valida la firma, lee la pasarela y la referencia del cobro, llama al procesamiento y traduce el resultado a la respuesta HTTP. Es lo único que sabe de HTTP.
       - **Procesamiento** (un servicio de aplicación): recibe "pasarela + referencia del cobro" y aplica esta regla D.4 (consulta, traducción, bloqueos, tabla de decisión y efectos). No sabe de HTTP ni de quién lo llamó.
       - Así, el mismo procesamiento lo usan el aviso, la conciliación (A.17) y, si se adopta, la opción B.
     - **Opción B, para más adelante: responder primero y procesar después.**
       - **Cómo sería:** la recepción guarda el aviso en una tabla de avisos recibidos y responde 200 de inmediato; un proceso programado recorre esa tabla, llama al **mismo** servicio de procesamiento y reintenta los que fallen.
       - **Por qué sería bueno:**
         - siempre respondemos a tiempo, aunque la pasarela o la base estén lentas;
         - la confirmación de un pedido no queda esperando los 15 minutos del siguiente reintento de la pasarela;
         - los reintentos los controlamos nosotros (frecuencia, límite y alerta), en lugar de depender de una política de la pasarela que no documenta su final;
         - deja un registro propio de cada aviso recibido, útil para revisar casos raros.
       - **Cuándo conviene pasar a B:** si en producción hay avisos que se acercan o pasan de los 22 segundos, o confirmaciones que se retrasan por esa causa, o si se quiere controlar los reintentos.
       - **Qué cuesta, con el requisito de separación cumplido:** una migración para la tabla, cambiar la recepción para que guarde y responda, el proceso programado con sus reintentos, y sus pruebas. Más o menos un PR pequeño. El PR 3 trae la infraestructura de procesos programados del job, así que después del PR 3 es más barato.
       - **Qué no cambia:** la tabla de decisión, el criterio común de D.10, los bloqueos, la conciliación y las pruebas del procesamiento.
5. **Lo que debe pasar junto, pasa junto (ADR 0014, punto 4, tipo 1).** El cambio del pago, el cambio del pedido y la liberación de la franja van en una sola transacción. Si un paso falla, no queda ninguno.
6. **Las llamadas a Mercado Pago van fuera de las transacciones** (ADR 0014, punto 4 y precisión del 2026-11-12). Primero se consulta o se actúa en la pasarela; después se abre una transacción corta.
7. **Orden de bloqueo: pedido → pago → franja.** Todo proceso que modifique un pago o un pedido toma los bloqueos de fila en ese orden. El pedido es el punto de encuentro, porque un pedido puede tener varios pagos.
8. **Actualizaciones condicionales de una sola fila.** `attachCheckout` y `markFailed` actualizan solo si el pago sigue en el estado esperado, sin reescribir la fila entera, para no pisar un estado que otro proceso ya cerró.
9. **El job revisa el pedido antes de liberar (precisada el 2026-11-13).**
   - Después de tomar el bloqueo del pedido, dentro de la transacción y antes de liberar la franja, el job vuelve a comprobar **el estado del pedido y `hold_expires_at`**.
   - Si el pedido ya no está `PENDING_PAYMENT`, o si su retención ya no está vencida (porque un intento nuevo la extendió), no libera.
   - Nunca decide con lo que leyó antes de tomar el bloqueo.
   - Junto con A.9 y A.11, crear un intento y liberar la franja se excluyen: crear un intento exige la retención vigente, liberar exige la retención vencida, y las dos comprobaciones se hacen con el mismo bloqueo del pedido.
10. **Invariante: una aprobación y la transición de su pedido se guardan juntas (aprobado el 2026-11-13; precisado el 2026-11-14).**
    - Al terminar cualquier transacción, nunca hay un pago `PAID` cuyo pedido esté en `PENDING_PAYMENT` o en `PENDING_PAYMENT_NO_SLOT`.
    - **`PAID` significa aprobación verificada con la pasarela:** solo se escribe después de consultarla (D.2).
    - **Criterio común para decidir el pedido (aprobado el 2026-11-14).** El aviso, el job y la conciliación usan la misma regla, con intentos abiertos o cerrados.
      - Con el pedido bloqueado, se vuelve a leer su estado.
      - **Decide si la franja sigue asignada, no si venció la retención:** `PENDING_PAYMENT` → `CONFIRMED`, y conserva la franja; `PENDING_PAYMENT_NO_SLOT` → `PAID_WITHOUT_SLOT`.
      - La retención es el plazo para crear intentos (A.9) y para que el job libere la franja (D.9). No es la propiedad de la franja: mientras no se libera, la franja sigue contada para el pedido.
      - **Comprobación defensiva:** si el pedido está `PENDING_PAYMENT` pero no tiene la franja asignada, el invariante de D.5 está roto. No se confirma; se registra un error operativo para revisión manual.
      - La ventana en que se confirma con la retención vencida es lo que tarda el job en liberar: su frecuencia, la espera de A.19 y los casos que no liberan (A.14, A.15). Si el job no corre, la ventana se alarga (P17).
    - Cuando el estado del pedido admite la transición, se guarda en la misma transacción que el pago, con el pedido bloqueado (D.5 y D.7). Si falla, se deshace todo: el pago sigue en su estado anterior, el aviso responde un error y la pasarela reintenta, o el job o la conciliación lo resuelven en su siguiente pasada.
    - **Cuando el estado del pedido no admite transición** (`CANCELLED`, o ya pagado por otro intento), el pago se registra `PAID` igual, el pedido no cambia y se registran un error operativo y una alerta (A.21 y A.22).
    - Para que se cumpla, ningún camino escribe `PAID` sin pasar por la misma operación que decide el pedido: el aviso, el job, la conciliación y el cambio de opción usan un único punto de decisión.
    - **Si el invariante aparece roto** (por un error de código o un cambio manual en la base), el job no cancela ni libera. Verifica el cobro con Mercado Pago antes de reparar: si confirma la aprobación, completa la confirmación del pedido sin tocar el pago; si no la confirma, no cambia nada y lo deja para revisión manual (A.13).
    - Hoy no hay código del aviso. El invariante es de diseño y se demuestra con pruebas al implementar.

### E. Coordinación entre procesos

Las reglas D.6 a D.9 se aplican a todos. Lo que falta definir de cada proceso está en la última columna.

| Proceso | Qué modifica | Lo aprobado | Pendiente |
|---|---|---|---|
| Iniciar un pago (`reserve`) | Inserta un pago y cambia la retención del pedido | Bloquear el pedido primero (D.7) y, dentro de la transacción, revisar su estado, su retención y el tope de intentos (A.8 y A.9). Si hay un pago `PENDING` sin referencia con el plazo de gracia vencido, cerrarlo antes (D.3) | — |
| Crear el cobro | Nada en la base | Fuera de toda transacción (D.6) | — |
| `attachCheckout` | Referencia y URL del pago | Actualización condicional (D.8) | — |
| `markFailed` | Estado del pago | Actualización condicional (D.8); también ante una falta de respuesta al crear (D.3, P6); guarda el motivo (P5) | — |
| Aviso de Mercado Pago | Pago y pedido; nunca la franja (D.4) | D.2; la regla central de D.4; D.5 a D.7; criterio común de D.10; A.21 y A.22 | — |
| Job de vencimiento (PR 3) | Pago, pedido y franja | D.6, D.7 y D.9; C2 para `processing / in_process` y `processing / pending_review_manual`; A.13 a A.15; A.18 a A.20 | El módulo dueño (P12); qué pasa si no corre (P17); el mecanismo de las alertas (P11) |
| Conciliación | Pago y pedido; nunca la franja | D.6, D.7 y D.10; A.17 | El módulo dueño (como P12); el mecanismo de las alertas (P11) |
| Cambio de opción de pago (PR 2b, después del PR 2; hasta entonces, 409) | Pago y un pago nuevo | D.6 y D.7; cierre verificado del cobro anterior antes de crear el nuevo; reutiliza la consulta de D.4, el punto de decisión de D.10 y la cancelación de A.13 (P13, partes 1 y 2); cerrar el anterior y guardar el nuevo, en una sola transacción; el intento cerrado cuenta para el tope de A.9 (P13, partes 1 a 3) | — |

### F. Detalles de implementación (orientativos; se verifican al implementar)

**Cambios de datos previstos** (cada uno es una migración nueva; ninguna se ejecuta con este ADR):
- `PENDING_PAYMENT_NO_SLOT` en la restricción `ck_order_status` (creada en la migración V6).
- `REFUNDED` en la restricción `ck_payment_status` (creada en la migración V8).
- Un campo en `payment` para el último estado del proveedor (D.1). Nombre y forma, por definir al implementar. El ejemplo aprobado (`in_process`) es un **detalle**, no un estado: si el campo solo guardara el `status`, no lo conservaría.
- Un campo en el pedido con el **instante en que entró en `PENDING_PAYMENT_NO_SLOT`**. Se fija una sola vez, en esa transición y en la misma transacción, y no se reinicia. La alerta de 24 horas de A.17 se cuenta desde ahí.
- Un campo en el pago con el **motivo**, distinto del estado del proveedor (D.1): por qué sigue `PENDING` (C2, A.19, A.20) o por qué quedó `FAILED` (P5). Lo usan los mensajes de A.23 y la operación.
- El **instante de vencimiento del cobro** (creación más `expiration_time`), guardado o calculado, para A.18 y A.19.
- Se conserva la **URL del checkout**, que hoy ya guarda `attachCheckout`, para el botón "Volver a pagar" (A.23).
- Ajustes nuevos en la base: el margen de A.19 (15 minutos), la frecuencia de la conciliación (1 hora) y su alerta (24 horas).

*Recomendaciones sin aprobar:*
- Validar `payment.mercadopago.checkout-expiration` al arrancar, con un máximo (por ejemplo, no más que los minutos de la opción). A.19 supone un vencimiento corto; el valor de 24 horas usado en V1 era solo para la prueba.
- Registrar cada par de estado y detalle recibido de la pasarela y alertar ante uno no visto antes, para detectar si producción difiere del sandbox.

**Hechos del código actual que habrá que cambiar** (revisados el 2026-11-12 en `main`, 6f46a3c):
- `StartPaymentService.start` revisa el estado del pedido fuera de la transacción, y `PaymentAttemptService.reserve` ignora que `OrderHoldService.extendHold` no pudo extender la retención. Es la carrera del contexto.
- `PaymentAttemptService.attachCheckout` y `markFailed` guardan la fila entera con `save()`. No cumplen D.8.
- No hay bloqueo de fila sobre pedidos ni sobre pagos (D.7).
- No existe la operación de liberar una franja. `ReserveSlotService` solo reserva.
- El pedido no tiene reglas de transición (sección C).
- `StartPaymentService.start` marca `FAILED` ante cualquier excepción de la pasarela, incluidos los tiempos agotados. Con P6 eso es correcto (D.3); falta guardar el motivo (P5).
  - No pone dinero en riesgo: la URL de pago solo llega al cliente después de `attachCheckout`, así que nadie puede pagar un intento que no tiene referencia.
- La espera por una conexión a la base es la de HikariCP por defecto (30 s), y no hay un tiempo máximo de espera de bloqueos (`lock_timeout`). Los dos pasan de los límites del aviso (D.4, P16).
- `MercadoPagoGateway` usa un solo cliente HTTP con 5 s de conexión y 15 s de respuesta. La consulta del aviso necesita 3 s y 8 s (D.4).
- Si ya hay un pago `PENDING` sin cobro, `StartPaymentService.start` responde que el cobro se está creando, sin plazo. Falta cerrar el intento a medio crear pasado el plazo de gracia (D.3).
- **Ningún componente cambia hoy el estado de un pedido de forma indirecta.**
  - El único que escribe el estado es `CreateOrderService`, al crear el pedido.
  - `OrderPersistenceAdapter.save` solo inserta.
  - No hay escuchadores de eventos ni tareas programadas.
  - Esto vale para el código actual. El job, la bandeja de salida y la consola agregarán escrituras, y deben respetar la decisión A.4.

**Cuidado con Hibernate.** Dentro de la transacción, la primera lectura del pago debe ser la que lo bloquea. Si antes se leyó sin bloqueo, Hibernate devuelve la copia que ya tenía en memoria, con el estado viejo.

**Pruebas que demuestran las reglas** (PostgreSQL real; WireMock en lugar de Mercado Pago):
- Dos avisos simultáneos del mismo pago: un solo cambio y un solo efecto (D.4).
- Un aviso que confirma mientras se inicia otro pago: no se crea un segundo cobro (D.7).
- El job y un aviso al mismo tiempo: no se libera la franja de un pedido confirmado (D.9).
- `attachCheckout` o `markFailed` tardíos no pisan un estado final (D.8).
- C2: retención vencida con `processing / in_process` y con `processing / pending_review_manual`, y después aprobación (A.2).
- Reembolso: el pago queda `REFUNDED` y el pedido no cambia (A.4).
- `failed / expired` con y sin cada una de las tres condiciones de A.18.
- `waiting_retry` antes y después del margen, con cada fila de A.19.
- `failed / failed` al vencer la retención (A.20).
- Aprobación de un intento `EXPIRED` en cada estado del pedido de A.21, incluido con otro intento `PENDING`.
- Aprobación con `hold_expires_at` vencido y la franja todavía asignada: el pedido queda `CONFIRMED`, por el aviso, por el job y por la conciliación (D.10).
- Aprobación cuando el job ya liberó la franja: `PAID_WITHOUT_SLOT`.
- Pedido `PENDING_PAYMENT` sin la franja asignada: no se confirma; se registra un error operativo (D.10).
- Dos pagos aprobados en un mismo pedido, y una order con dos transacciones aprobadas: un solo `PAID` por order, error operativo y alerta (A.22).
- La conciliación no pisa lo que ya resolvió un aviso (A.17).
- Si la pasarela no responde al crear el cobro, o falla el guardado de `attachCheckout`, el cliente no recibe la `checkout_url`, y la URL no aparece en los logs ni en el mensaje de error (D.3).
- Un intento `PENDING` sin referencia, pasado el plazo de gracia, se cierra como `FAILED` al iniciar otro pago y en el job (D.3).
- El aviso responde 503 sin cambiar nada si la pasarela no responde, si no hay conexión a la base a tiempo o si el bloqueo tarda más de su límite (D.4).
- El servicio de procesamiento del aviso se prueba sin HTTP, con "pasarela + referencia del cobro" (D.4).
- Un aviso con una referencia desconocida responde 200, no consulta a la pasarela y no crea ningún pago (D.4).
- Un aviso con la firma inválida, o sin firma, responde 401 y no consulta a la pasarela; sin clave configurada se rechazan todos (D.4).

### G. Cuestiones pendientes

Salvo las marcadas como resueltas, ninguna de estas cuestiones está decidida. Las alternativas se listan sin elegir.

- **P1. Resuelta el 2026-11-13:** varios intentos por pedido, con límite (A.8 y A.9).
  - *Por qué la retención no se extiende sin fin* (comprobado en el código el 2026-11-12):
    - `hold_limit_at` se fija al crear el pedido, y la entidad lo marca `updatable = false`;
    - la base exige `hold_expires_at <= hold_limit_at` (`ck_order_hold`, migración V9);
    - la consulta `updateHold` vuelve a comprobar el tope.

    La **fecha** no pasa de los 51 minutos. La **liberación** depende de que corra el job (P17).
  - *Queda para la segunda etapa:* reconciliar el ADR 0014, cuyo punto 3 dice "reintentar crea un pedido nuevo".
- **P2. Resuelta el 2026-11-13:** la franja se mantiene hasta que vence la retención del pedido (A.10). Coincide con el código actual, en el que `extendHold` nunca acorta.
  - La extensión de la retención en cada intento quedó aprobada en A.11.
  - *Queda para la segunda etapa:* reconciliar las consecuencias del ADR 0013 ("se libera el cupo apenas el proveedor rechaza o vence").
- **P3. Resuelta el 2026-11-13:** el vencimiento del cobro no cancela el pedido; lo cancela el vencimiento de la retención (A.12).
  - *Queda para la segunda etapa:* reconciliar el ADR 0014 (punto 3: "si el pago vence, el pedido se cancela"). La decisión sigue al ADR 0012 (punto 9: vencer el cupo es asunto del pedido; vencer el pago, del proveedor).
- **P4. Resuelta el 2026-11-15:** la regla central del aviso está en D.4, con su secuencia y su tabla de decisión. Reemplaza el ADR 0014, punto 5.
- **P5. Resuelta el 2026-11-14:** un solo estado `FAILED`, más un motivo en el campo de motivo del pago (F).
  - `FAILED` solo significa que **no se pudo crear el cobro**. El rechazo del banco no lleva a `FAILED`: el pago sigue `PENDING`, porque el cliente puede reintentar en el mismo checkout (A.19 y A.20).
  - Motivos de `FAILED`:
    - **"rechazada por la pasarela":** la pasarela respondió que la petición no era válida (por ejemplo, un monto con centavos);
    - **"creación sin confirmar":** la pasarela no respondió, o el intento quedó a medio crear (D.3, P6).
  - Para el cliente no cambia nada: en los dos casos ve "No pudimos iniciar el pago. Intenta de nuevo" (A.23). El motivo sirve para la operación.
  - Se descartan dos estados (`FAILED_BY_BANK` y `FAILED_BY_SYSTEM`): el del banco nunca se usaría, y el otro ya es `FAILED`. No hace falta migrar el estado del pago por esto.
- **P6. Resuelta el 2026-11-14:** no hay estado `UNKNOWN`. Una creación de cobro sin respuesta, o un intento a medio crear pasado el plazo de gracia, se cierra como `FAILED` con el motivo "creación sin confirmar", porque nadie puede pagarlo. Una consulta o cancelación sin respuesta no cambia nada (D.3).
  - **Mejora futura, cuando el sistema esté terminado:** recuperar el enlace en lugar de mostrar el error. Si Mercado Pago no responde al crear el cobro, se repite la petición con la misma llave de idempotencia. Si Mercado Pago ya lo había creado, devuelve el mismo enlace y el cliente va a pagar sin ver un error ni volver a pulsar "Pagar".
    - Solo ayuda cuando Mercado Pago tarda, no cuando se cae nuestra aplicación.
    - Puede duplicar la espera (hasta unos 30 segundos) antes del error.
    - Antes hay que comprobar V4.
    - Es segura, porque solo un enlace llega al cliente, y se añade sin cambiar lo decidido.
- **P7. Otros estados del proveedor al vencer la retención.**
  - *Resuelto:*
    - el caso 1, el cobro sigue abierto (`created`), en A.13, junto con la regla de no liberar con un resultado desconocido;
    - el caso 2, `processing / pending_review_manual`, que se incorpora a C2 (A.2);
    - el caso 3, `action_required / waiting_capture`, como estado no soportado (A.14);
    - el caso 4, cuando Mercado Pago no responde: no se libera y hay una alerta a los 15 minutos de `hold_expires_at` (A.15).
  - **P7 queda resuelta.** Queda pendiente el mecanismo de las alertas (P11).
- **P8. Resuelta el 2026-11-13:** el pedido pasa a `CANCELLED`, solo con un estado definitivo reconocido (A.16). Quedan aparte el mensaje al cliente (P11); quién revisa estos pedidos si no llega el aviso quedó en A.17.
- **P9. Un reembolso visto con el pago todavía `PENDING`.**
  - *Falta:* qué hacer si el primer estado definitivo que vemos es `refunded` (por ejemplo, se perdió el aviso de la aprobación).
  - *Alternativas:*
    - registrar la aprobación y luego el reembolso, con lo que el pedido quedaría confirmado;
    - pasar el pago directo a `REFUNDED`, sin confirmar el pedido.
  - *Condiciona:* la tabla B. No cambia la decisión A.4, que cubre `PAID → REFUNDED`.
  - *Mientras tanto:* el aviso no cambia nada y genera un error operativo y una alerta (D.4).
- **P10. La gestión de la florista sobre `PAID_WITHOUT_SLOT`.**
  - *Falta:* si el pedido sale algún día de ese estado en el sistema (a `CONFIRMED` con otra franja, o a `CANCELLED`), o si se gestiona por fuera.
  - *Condiciona:* la fila `PAID_WITHOUT_SLOT → ?` (C). No bloquea el PR 2; la consola llega en la fase 4.
- **P11. El mecanismo de los avisos a la florista y al cliente.**
  - *Falta:* cómo y en qué PR se avisa (A.4, A.5 y A.7).
  - *Alternativas:*
    - en los PR de pagos solo queda el estado registrado, y los avisos llegan con el correo;
    - adelantar la bandeja de salida (ADR 0014, punto 4).
  - *Contradicción:* el ADR 0014 (punto 4) dice que "los PR de pagos no la necesitan".
- **P12. El módulo dueño del job.**
  - *Falta:* en qué módulo vive el job de vencimiento.
  - *Contradicción:* el ADR 0012 (punto 9) dice que vencer el cupo es asunto del pedido y que antes se consulta al proveedor. Si el job vive en pedidos, pedidos depende de pagos, y el ADR 0014 (punto 4) exige "pagos depende de pedidos… sin ciclos".
  - *Alternativas:*
    - el job vive en pagos y llama a pedidos;
    - el job vive en pedidos y consulta al proveedor por un puerto que implementa pagos.
- **P13. Resuelta el 2026-11-15: el cambio de opción de pago** (cambiar de medio o de pasarela con un pago `PENDING`), en tres partes.
  - *Por qué hace falta:* el checkout de Mercado Pago solo muestra el medio que el cliente eligió (`not_allowed_types` excluye los demás). "Pagar con otro medio" dentro del checkout solo permite otra tarjeta, no pasar de tarjeta a PSE.
  - *Resuelto (parte 1, 2026-11-15): se implementa en un PR propio, el **PR 2b**, después de integrar el PR 2.*
    - **Mientras tanto se mantiene el 409** actual ("ya tienes un pago en curso").
    - **Nunca hay dos cobros abiertos para un mismo pedido.** El cobro nuevo solo se crea después de **verificar que el anterior ya no se puede pagar**:
      - si el cobro anterior ya está cerrado (por ejemplo, venció), no hace falta cancelarlo;
      - si hay que cancelarlo, no basta con pedirlo: se vuelve a consultar y solo vale `canceled / canceled`, igual que en A.13;
      - si la consulta muestra que ya se pagó, no se crea nada y se aplica la aprobación (D.10);
      - si la pasarela no responde, o la respuesta es ambigua, no se crea nada.
    - **El PR 2b reutiliza la lógica del PR 2; no crea un segundo mecanismo:**
      - la consulta del estado y su traducción son las del adaptador del aviso (D.4);
      - aplicar el resultado pasa por el mismo punto de decisión de D.10 (que ya incluye el cambio de opción entre sus caminos);
      - la cancelación es la misma del job (A.13), con la misma llave de idempotencia por pago.
  - *Resuelto (parte 2, 2026-11-15): qué se hace según el estado del cobro anterior.* Si es la misma opción (el cliente recargó la página), se devuelve el mismo cobro, como hoy. Si es otra opción:

    | El cobro anterior está… | Qué se hace | Respuesta al cliente |
    |---|---|---|
    | Sin referencia, dentro del plazo de gracia (D.3) | Nada | 409: "Tu pago se está iniciando; espera un momento" |
    | Sin referencia, con el plazo de gracia vencido | Se cierra como `FAILED`, "creación sin confirmar" (D.3), y se crea el nuevo | Va al pago nuevo |
    | `created / created` | Se cancela (A.13), se vuelve a consultar y, solo con `canceled / canceled`, se crea el nuevo | Va al pago nuevo |
    | `action_required / waiting_retry` | Igual que `created`. Si la pasarela no lo deja cancelar, o no confirma `canceled / canceled`, no se crea nada (V6) | Si se pudo, va al pago nuevo. Si no, 409: "Tu pago anterior sigue abierto; podrás cambiar de medio cuando venza, a las HH:MM" (vencimiento del cobro) |
    | `processing / in_process` o `processing / pending_review_manual` | Nada: no se puede cancelar y puede aprobarse | 409: "Tu pago está en revisión; espera el resultado" |
    | Aprobado (`processed / accredited`) | Se aplica la aprobación (D.10). No se crea nada | "Tu pago ya se aprobó" |
    | Cerrado (D.4: `canceled / canceled`, `canceled / expired` o `failed / expired` con las condiciones de A.18) | Se registra el cierre (D.4) y se crea el nuevo | Va al pago nuevo |
    | `failed` sin cierre confirmado (A.20), no reconocido, respuesta ambigua o sin respuesta | Nada | 409 o 503: "No pudimos cambiar el medio de pago ahora; intenta en un momento" |

    - **Para crear el nuevo**, la retención tiene que seguir vigente y el pedido no puede haber llegado al tope de intentos (A.9), revisados con el pedido bloqueado. Si no se cumple, no se hace el cambio.
    - **Cerrar el anterior y guardar el nuevo intento van en la misma transacción**, con el pedido bloqueado y después el pago (D.7), con actualización condicional sobre el anterior (D.8). Así nada se cuela entre los dos pasos.
    - **Las llamadas a la pasarela** (consultar, cancelar y crear el cobro nuevo) van fuera de la transacción (D.6). Crear el cobro nuevo sigue los pasos del inicio de un pago (D.3).
    - **Sin probar:** si se puede cancelar una order en `waiting_retry` (V6). La tabla es segura igual, porque solo se crea el nuevo con `canceled / canceled` confirmado. Se prueba al construir el PR 2b.
  - *Resuelto (parte 3, 2026-11-15): el intento cerrado por cambiar de opción **cuenta** para el tope de 5 (A.9)*, porque llegó a crear un cobro.
    - El tope limita cuántos cobros abre un pedido, y cada cambio de opción abre uno. Si no contara, como los reintentos dentro del checkout tampoco cuentan, alguien podría abrir cobros sin límite durante los 51 minutos, por ejemplo para probar tarjetas robadas.
    - Si el pedido llega al tope, el cambio no se hace y el cliente ve "Llegaste al máximo de intentos de pago para este pedido" (A.23). Puede crear un pedido nuevo con su carrito (A.9).
    - Si 5 resulta poco en la práctica, se cambia el ajuste en la base de datos (A.9).
  - *Queda por actualizar:* `docs/plan.md` dice que el cambio de opción llega con el PR 2; el ADR 0014 (precisión del 2026-11-12) también, y se reconcilia en la segunda etapa.
- **P14. La tabla completa de traducción desde Mercado Pago.**
  - *Ya cubierto por decisiones aprobadas:*
    - `processed / accredited` → `PAID`;
    - `processing / in_process` y `processing / pending_review_manual` → sigue `PENDING` (A.2);
    - `canceled / expired` → `EXPIRED` (ADR 0012, punto 9);
    - `canceled / canceled` → `CANCELLED` (A.13 y A.16);
    - `refunded` → `REFUNDED` (A.4);
    - `action_required / waiting_capture` → sin cambios, error operativo y alerta (A.14);
    - `failed / expired` con condiciones → `EXPIRED` (A.18, provisional);
    - `failed / failed` y `failed` con otros detalles → sigue `PENDING` (A.20, provisional);
    - `action_required / waiting_retry` → sigue `PENDING` (A.19).
  - *Falta:*
    - `canceled` con otro detalle distinto de `expired` y `canceled`;
    - `processed / partially_refunded`;
    - `created` fuera del caso de A.13;
    - cualquier detalle de `processing` que no esté en A.2;
    - valores de transacción distintos de `failed`, `processed` y `processing` (hoy, no reconocidos: A.19).
- **P15. Resuelta el 2026-11-14:** una aprobación confirma el pedido aunque `hold_expires_at` haya pasado, mientras la franja siga asignada (criterio común de D.10). Coincide con la fila de aprobación de A.13.
- **P16. Resuelta el 2026-11-15:** las respuestas HTTP del aviso y los tiempos máximos, en D.4.
  - Parte 1: se procesa dentro de los 22 segundos, con tiempos máximos por paso, y la recepción va separada del procesamiento para poder pasar a "responder primero y procesar después".
  - Parte 2: ante una referencia desconocida se responde 200, se registra y nunca se crea un pago.
  - Parte 3: ante una firma inválida o ausente se responde 401, sin procesar nada; sin límite de antigüedad por ahora (V8).
- **P17. Si el job no corre.**
  - *Falta:* qué se hace si el job deja de correr. El tope de 51 minutos es una fecha, no una acción: la franja solo se libera cuando el job corre.
  - *Condiciona:* el alcance del job (E).
- **P18. Resuelta el 2026-11-14:** conciliación periódica, con alerta a las 24 horas (A.17).
- **P19. Los reintentos dentro del checkout de cada pasarela.**
  - *Resuelto para Mercado Pago:* pertenecen al mismo intento (A.8 y A.19).
  - *Falta para Wompi:* si nuestro intento corresponde a una referencia; cuándo se cierra (la última transacción definitiva, pasados los 3 minutos de reintento, sin otra `PENDING`); si su API permite buscar transacciones por referencia; qué medios se pueden cancelar.
  - *Lo que dice Wompi (consultado el 2026-11-13):* si el primer intento falla, el cliente tiene 3 minutos para pagar con otro medio; eso crea una segunda transacción con la misma referencia, y una transacción rechazada no pasa a aprobada.
  - Se decide al diseñar el adaptador de Wompi.
- **P20. Consulta a Mercado Pago sobre Checkout Pro vía Orders (no bloqueante).**
  - ¿`failed` (con detalle `failed` y con `expired`) es terminal?
  - ¿`failed / expired` y `action_required / waiting_retry` son estados oficiales de Checkout Pro?
  - ¿Por qué un mismo rechazo de prueba da a veces `processing` y a veces `waiting_retry`?
  - ¿Se puede cancelar una order en `waiting_retry`, y qué ve el cliente después?
  - ¿Se puede pagar entre el vencimiento del cobro y el cambio de estado?
  - ¿Una transacción rechazada puede aprobarse? ¿Puede haber dos transacciones aprobadas en una order?
  - ¿Hay un plazo máximo para `processing`?
  - ¿Qué avisos se envían en cada transición?
  - ¿Cuál es el `expiration_time` mínimo?
  - *Si la respuesta contradice A.18 a A.20, se revisan esas reglas y A.16.*

### H. Comprobaciones en el sandbox de Mercado Pago

- **V1. Incertidumbre documentada, no bloqueante (cerrada de forma provisional el 2026-11-14).** Pagar con un rechazo (OTHE) y después con una aprobación (APRO) en el mismo checkout. ¿`failed` es definitivo? ¿Cómo se comporta el reintento?
  - **Situación:** no se ha confirmado si `failed` es un estado terminal en Checkout Pro vía Orders. No bloquea la implementación: se maneja con las reglas operativas provisionales A.18 a A.21, y la consulta a Mercado Pago es P20.
  - **Se revisa si:** Mercado Pago responde P20; aparece en producción una aprobación sobre un intento cerrado; o llega un par de estado y detalle no visto antes.
  - *Las orders se nombran "Order 1" a "Order 4": sus ids reales del sandbox no se escriben en el repositorio (ADR 0002).*
  - **Observación del sandbox, 2026-11-13 (checkout de 15 minutos).** Dos pagos con la Mastercard de prueba, como invitado:

    | Order | Titular | Pago enviado | Estado inicial | Estado posterior | Detalle de la transacción | Último cambio |
    |---|---|---|---|---|---|---|
    | Order 1 | OTHE | ≈ 05:25 UTC | `processing / in_process` | `failed / failed` | `rejected_by_issuer` | 14:06:15 UTC |
    | Order 2 | FUND | ≈ 05:33 UTC | `processing / in_process` | `failed / failed` | `insufficient_amount` | 14:05:55 UTC |

    - La página de pago no mostró un rechazo, sino "Estamos procesando tu pago".
    - Las dos orders seguían `processing / in_process` en las consultas de las 05:26, 05:28, 05:33 y 06:09 UTC.
    - Las dos aparecieron `failed / failed` en la consulta de las 18:16 UTC.
    - El cambio ocurrió unas **8 horas y media** después del pago.
    - Al volver a abrir el enlace de pago de la order de OTHE, mientras estaba `processing`, Mercado Pago lo redirigió a "No se puede continuar con este pago". No fue posible reintentar con APRO en ese checkout.
    - **El motivo específico del rechazo está en la transacción** (`transactions.payments[].status_detail`), no en la order, cuyo detalle es solo `failed`.
  - **Observación del sandbox, 2026-11-13 y 2026-11-14 (checkout de 24 horas, solo para la prueba).**

    | Order | Prueba | Resultado |
    |---|---|---|
    | Order 3 | OTHE y después APRO en el mismo checkout | `action_required / waiting_retry` (transacción `failed / rejected_by_issuer`) y, a los 70 segundos, `processed / accredited` con dos transacciones |
    | Order 4 | OTHE, sin tocar después | `waiting_retry` desde las 18:33:38 del 2026-11-13; **`failed / expired`** a las 18:34:09 del 2026-11-14, 2 min 41 s después del vencimiento. Ninguna transacción nueva (consultado a las 21:45 del 2026-11-14) |

  - **Lo que demuestran las dos observaciones:**
    - la transición `processing → failed` existe, aunque no aparece en el ciclo documentado;
    - el rechazo de una transacción no cierra la order;
    - una order en `waiting_retry` admite otra transacción, que puede aprobarla;
    - al vencer, una order con transacciones rechazadas queda `failed / expired`.
  - **Lo que no demuestran:**
    - que `failed` sea irreversible;
    - por qué el mismo rechazo dio `processing` en una prueba y `waiting_retry` en la otra;
    - que los pagos reales tarden lo mismo, o que producción se comporte igual;
    - que `capture_mode: automatic_async` sea la causa de la demora.
  - **Comprobación pendiente, no bloqueante (V1a):** ¿se puede pagar una order en `failed / failed` con su checkout todavía vigente?
- **V2. Hecha el 2026-11-13.**
  - Una order `created`, sin pagar, se cancela con `POST /v1/orders/{id}/cancel`. Responde HTTP 200 y la order queda `canceled / canceled`. Su enlace de pago lleva a "No se puede continuar con este pago".
  - Una order pagada (`processed / accredited`) responde HTTP 409 con el código `cannot_cancel_order`, y no cambia nada. El mensaje dice que solo se cancelan orders `action_required` o `created`.
  - Lo confirma la referencia oficial del endpoint, consultada el 2026-11-13: solo se cancelan esas dos, el éxito es un 200 y `X-Idempotency-Key` es obligatoria.
  - Una cancelación nuestra deja el detalle `canceled`; un vencimiento deja `expired`. Así se distingue uno del otro.
- **V2b (pendiente).** Qué responde Mercado Pago al cancelar dos veces la misma order, con la misma llave y con otra distinta. La documentación no lo dice. El diseño de A.13 no depende de esa respuesta, porque ante cualquier cosa que no sea un 200 claro vuelve a consultar.
- **V3.** El nombre real del tema de los avisos (`order`, `orders` u `orders_v2`; las páginas oficiales no coinciden), y si los reembolsos (`refunded`) llegan por el mismo tema.
- **V5. Hecha el 2026-11-13.** Tres orders creadas por nuestra integración (una aprobada, una en revisión y una cancelada) devuelven `type: online`, `processing_mode: manual` y `capture_mode: automatic_async` al consultarlas con `GET /v1/orders/{id}`. Se leyó con la consulta, no con la respuesta de creación, que no guardamos; según la guía de Checkout Pro, la respuesta de creación trae el mismo valor.
  - *Dato relacionado con V1, sin comprobar:* la referencia dice que con `automatic_async` "la order puede permanecer en status=processing esperando actualización asíncrona". Podría explicar por qué OTHE y FUND quedaron `processing / in_process`. Es una hipótesis, no una conclusión.
- **V4 (para la mejora futura de P6; no bloquea).** Repetir la creación de un cobro con la misma llave de idempotencia: ¿devuelve la misma order? Solo hace falta para la mejora futura de P6.
- **V6 (propuesta, no bloqueante).** Cancelar una order en `waiting_retry`: qué estado queda y qué ve el cliente en la pestaña abierta.
- **V7 (propuesta, no bloqueante).** El `expiration_time` mínimo que acepta Mercado Pago.
- **V8 (pendiente, se hace al implementar el PR 2).** El formato exacto de la firma de los avisos de Orders: cómo se arma el texto que se firma (con `x-request-id`, el id del recurso y `ts`), y si `ts` cambia en cada reintento. La página de avisos lo explica en una pestaña ("Sin SDKs") que no se pudo leer. Se comprueba con un aviso real del sandbox. Con el resultado se revisa si conviene limitar la antigüedad del aviso (D.4).

## Relación con otros ADR (reconciliación pendiente)
Este borrador no modifica los ADR anteriores. Cuando se apruebe, cada uno recibirá una línea de estado que remita al 0016, y los puntos de abajo quedarán reconciliados según las decisiones pendientes.

| ADR y punto | Qué cambia o choca | Depende de |
|---|---|---|
| 0003, estados | Falta `REFUNDED`. `FAILED` queda con un solo sentido, no se pudo crear el cobro, más un motivo (P5). No hace falta un estado para el resultado desconocido (P6, D.3) | A.4, P5, P6 |
| 0003, bosquejo del puerto (`fetchStatus` devuelve `PaymentStatus`) | Devolvería nuestro estado. La traducción no es uno a uno (D.1), depende de los dos niveles y de las transacciones (A.18, A.19, A.22) | — |
| 0003, ciclo de estados (`PENDING`, `PAID`, `FAILED`, `EXPIRED`, `CANCELLED`) | Un intento cerrado puede pasar a `PAID` con una aprobación verificada (A.21) | A.21 |
| 0009, precisiones del módulo `payment` ("un pago en un estado final no cambia nunca") | **Reconciliado el 2026-11-15** con una precisión en el 0009: las excepciones son `PAID → REFUNDED` (A.4) y la aprobación verificada de un intento cerrado (A.21). La lista vive en `domain`; cuándo se aplica, en `application` | A.4, A.21 |
| 0012, punto 1.6 ("se deshace todo: se libera el cupo y se cancela el pedido") | Choca con C2, que no cancela el pedido, y con A.8, que permite reintentar sobre el mismo pedido | A.2, A.8, A.12 |
| 0012, punto 9 | Hay que documentar C2, los dos vencimientos y lo que hace el job al vencer la retención. "`EXPIRED` cuando el proveedor confirma que ya no se puede pagar": `failed / expired` se interpreta como esa confirmación por una regla operativa, no documental | A.2, A.12 a A.15, A.18, P20 |
| 0012, punto 10 | La regla central de los avisos está en D.4; hay que documentar `PENDING_PAYMENT_NO_SLOT`. Se extiende a las aprobaciones de intentos cerrados (A.21); `PAID_WITHOUT_SLOT` sigue significando que el cupo ya se había liberado (D.10) | A.2, A.3, A.21, D.4, D.10 |
| 0012, punto 11 (avisos al cliente) | El mecanismo no existe en los PR de pagos | P11 |
| 0013, punto 5 y consecuencias ("se libera el cupo apenas el proveedor rechaza o vence") | Deja de ser cierto: tras un rechazo, la franja sigue reservada hasta que vence la retención (A.8 y A.10). Además, un rechazo de una transacción ni siquiera cierra el intento (A.8, A.19, A.20) | A.8, A.10, A.19, A.20 |
| 0013, punto 5 y fuentes (`in_process` "hasta 2 días hábiles"; un pendiente "se cancela a los 30 días") | Son datos de Payments API, no de Checkout Pro vía Orders. No se usan como garantía (A.17, P20) | A.17, P20 |
| 0013, pendiente ("qué pasa si el cliente intenta pagar después de `expiration_date_to`") | Respondido en parte: el enlace de una order `created` deja de funcionar al vencer (V2); una order en `waiting_retry` pasa a `failed / expired` (V1). Lo demás queda en P20 | V1, V2, P20 |
| 0014, punto 3 | "Reintentar crea un pedido nuevo" choca con A.8; "si el pago se rechaza o vence, el pedido se cancela" choca con A.10, A.12, A.19 y A.20. "Un pedido nunca tiene dos pagos activos" se mantiene: "activo" significa `PENDING`, porque no hay estado para el resultado desconocido (P6) | A.8, A.10, A.12, A.19, A.20, P6 |
| 0014, punto 4, tipo 1 ("el pago queda rechazado, el pedido cancelado y el lugar devuelto") | Un rechazo no cancela el pedido (A.10, A.19, A.20). Las salidas de un pedido son las de C | A.10, A.19, A.20, C |
| 0014, punto 4 ("dos operaciones"; "los PR de pagos no necesitan la bandeja de salida"; "sin ciclos") | Pedidos necesita más operaciones para pagos: confirmar, pasar a sin franja, `PAID_WITHOUT_SLOT`, cancelar y registrar un pago sin transición (D.10); los avisos; el dueño del job | D.10, P11, P12 |
| 0014, punto 5 ("si no está pendiente, se ignora") | Lo reemplaza D.4: no se ignoran los reembolsos ni las aprobaciones de intentos cerrados (A.21) | A.4, A.21, D.4 |
| 0014, precisión del 2026-11-12, paso 3 ("marcar el intento `FAILED` si algo falló") | Se mantiene, también ante un tiempo agotado (P6, D.3); falta guardar el motivo (P5). Un `FAILED` con referencia puede aprobarse después (A.21) | D.3, A.21, P5, P6 |
| 0014, precisión del 2026-11-12 ("cambiar de opción con un pago en curso responde 409… esperar a que venza"; "eso llega con el PR 2") | El 409 se mantiene hasta el PR 2b, no el PR 2 (P13). Con `waiting_retry`, el cliente vuelve al mismo checkout, pero solo puede usar otra tarjeta, no otro medio (A.8, A.23) | A.8, A.23, P13 |

## Consecuencias
- **A favor:**
  - Un solo lugar describe cómo cambian un pago y un pedido, y qué no puede pasar: un estado final que se reabre, una franja liberada de un pedido confirmado, un aviso repetido que repite efectos.
  - El pago en revisión ya no obliga a cancelar ni a retener la franja indefinidamente.
  - El estado del proveedor queda visible sin multiplicar los estados internos.
- **En contra:**
  - Un estado más en el pedido y otro en el pago, cada uno con su migración.
  - Hay que cambiar código del PR 1b (D.7 y D.8) antes de construir el aviso.
  - Varias secciones quedan condicionadas hasta resolver las cuestiones pendientes y las comprobaciones en el sandbox.
  - La florista asume a mano los pagos sin franja, los reembolsos y las compras duplicadas.
- **Por las reglas provisionales sobre `failed` (2026-11-14):**
  - *A favor:* V1 deja de bloquear la implementación sin afirmar nada que no sepamos; lo desconocido sigue sin liberar la franja; ninguna franja queda retenida indefinidamente por `waiting_retry`; el dinero que llega siempre queda registrado.
  - *En contra:* tres reglas provisionales (A.18, A.20 y la parte provisional de A.16) que habrá que revisar cuando responda Mercado Pago; pedidos sin franja que no se cierran solos (A.20); una aprobación tardía de un intento cerrado solo se ve si llega un aviso; más casos a mano para la florista (duplicados, aprobaciones sobre pedidos cancelados y alertas).

## Fuentes
- Mercado Pago, "Estado de la order" (Checkout Pro vía Orders API), consultada el 2026-11-12: https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/payment-management/status/order-status
- Mercado Pago, "Webhooks" (avisos de Orders), consultada el 2026-11-12 y el 2026-11-15 (respuesta 200 o 201 en 22 segundos; reintentos cada 15 minutos): https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/resources/notifications/webhooks
- Pruebas en el sandbox del 2026-11-12: un pago aprobado quedó `processed / accredited`; un cobro sin pagar dejó de abrirse a los 15 minutos y la API informó `canceled / expired` unos 2,5 minutos después.
- Mercado Pago, "Estado de la order" (Checkout API vía Orders), solo como referencia de otra integración: https://www.mercadopago.com.co/developers/es/docs/checkout-api-orders/payment-management/status/order-status
- Mercado Pago, referencia "Cancelar order": https://www.mercadopago.com.co/developers/es/reference/orders/online-payments/cancel-order/post
- Wompi, "Reintento de pago": https://docs.wompi.co/docs/colombia/reintento-de-pago/
- Pruebas en el sandbox del 2026-11-13 y del 2026-11-14 (V1).
