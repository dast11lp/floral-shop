# 0021. Quién recibe, mensaje de la tarjeta y nota de entrega (complementa el 0012, el 0013 y el 0020)

**Estado:** propuesta, borrador para revisión · **Fecha:** 2026-11-18

## Contexto
En una floristería casi siempre compra una persona y recibe otra. El diseño de la base de datos preveía para la fase 3 "Comprador y destinatario: se guardan por separado, con mensaje de tarjeta" y una nota del pedido. El pedido se construyó sin las tres cosas (pendiente 51):
- `customer_order` (migración V6) guarda nombre, correo y teléfono de quien compra, la dirección, la zona y la franja.
- Se asume que quien compra es quien recibe.
- No hay dónde guardar el mensaje de la tarjeta ni indicaciones para la entrega.

Hay que resolverlo antes de diseñar el carrito y el último paso, porque cambia lo que se pide y lo que guarda el pedido.

**Qué dicen los expertos y otras floristerías** (revisado el 2026-11-18):
- **Baymard:** cuando la compra es un regalo, los campos tienen que decirlo ("Dirección de quien recibe", no "Tu dirección"), y conviene que desde el carrito se vea que se puede mandar como regalo. En sus pruebas, más de la mitad de quienes mandaban un regalo agregaron un mensaje personal (dato tomado de una fuente secundaria).
- **Floristerías en línea:** el largo del mensaje depende de la tarjeta impresa (Fig & Bloom, 200 caracteres; Windflower, 300). Los pedidos anónimos igual piden el teléfono de quien compra, por si hay problemas con la entrega (UrbanStems). El mensaje casi nunca se puede cambiar después de que se empieza a armar el pedido (Euroflorist).

### Cómo leer este documento
- **Decisión:** lo decidido el 2026-11-18, una decisión por vez.
- **Detalles orientativos:** recomendaciones sin aprobar. Se confirman al construir.
- **Pendiente:** lo que falta decidir.

## Decisión

**1. Siempre se pregunta quién recibe.**
- Nombre y teléfono de quien recibe, obligatorios. El teléfono sirve para coordinar la entrega (portería, nadie en casa), y la pantalla dice para qué se usa.
- Opción **"Soy yo"**, que copia los datos de quien compra. Es solo de la pantalla: el backend siempre recibe los dos juegos de datos.
- Quien compra sigue dando su nombre, correo y teléfono, aunque la tarjeta vaya anónima: es a quien se le avisa del pago y de cualquier problema.

**2. El mensaje de la tarjeta.**
- **Opcional** y **uno por pedido**: un pedido es una sola entrega (ADR 0012), así que lleva una sola tarjeta.
- **La firma va aparte.** Si se deja vacía, la tarjeta sale anónima.
- **El largo máximo lo decide la florista**, según la tarjeta que use. Es un ajuste nuevo en `order_settings` (los ajustes de pedidos, ADR 0013, punto 3), editable en la consola, con **250 caracteres** de arranque.
- Se valida **al crear el pedido** contra el límite vigente, en el caso de uso (ADR 0009). Si ella lo baja después, los pedidos que ya existen conservan su mensaje.
- La columna tiene un **tope técnico más amplio** (1000 caracteres) para que cambiar el ajuste no pida una migración. El ajuste no puede pasar de ese tope.

**3. La nota de entrega.**
- **Opcional y corta,** unos 200 caracteres fijos, solo para indicaciones: "Torre 2, dejar en portería", "Llamar antes".
- El detalle de "A tu gusto" (qué flores, qué colores) **no va aquí**: es de un ramo, no del pedido. Se resuelve cuando exista ese producto.

**4. El orden del último paso** (precisa el punto 6 del 0020; ajustado el 2026-11-19 al diseñar el último paso):
- Son **dos pantallas**, con el indicador "Carrito › Entrega › Pago".
- **Entrega**, en este orden:
  1. **Tus datos:** quien compra.
  2. **Para quién:** quien recibe y el mensaje.
  3. **Dónde:** zona, dirección y nota de entrega.
  4. **Cuándo:** día y hora, consultados de nuevo al llegar a esta parte. El día llega resumido en una línea con "Cambiar día" y las horas van en lista; si no llega día o ya no sirve, se ve la fila de días abierta (pendiente 52, punto 6).
  5. **"Continuar al pago":** lleva a la revisión. **No crea nada todavía.**
- **Lo opcional que la mayoría no usa va plegado** con un botón "+ …" que abre el campo y lo deja abierto (decidido el 2026-11-19, pendiente 52, punto 8): la nota de entrega siempre, y el mensaje con la firma solo con "Lo recibo yo". Con "Es un regalo" el mensaje se ve de entrada, porque la mayoría de quienes regalan escriben uno.
- **Pago:** revisión de lo elegido (productos, día y hora, quien recibe, total con envío), opciones de pago y "Pagar … en Mercado Pago". Cada bloque tiene "Cambiar", que vuelve a Entrega con todo lo escrito intacto (vive en el navegador). Al entrar se vuelve a consultar el día y la hora.
- **El pedido se crea al pulsar "Pagar … en Mercado Pago".** En un solo toque se crea el pedido y se aparta el cupo, se inicia el pago y se va a Mercado Pago. Todo eso ya existe en el backend.

Por qué así:
- **Volver atrás para corregir no afecta al backend.** Si el pedido se creara en "Continuar al pago", corregir exigiría modificar o cancelar un pedido sin pagar (dos operaciones que no existen), o dejaría pedidos de más apartando cupo.
- **Hay menos cupo apartado sin compra:** quien llega a revisar y se va no aparta nada.
- **Entre elegir la hora y apartarla pasa solo lo que tarda revisar,** no los minutos de escribir el mensaje. Si la hora se llenó en ese rato, al pulsar "Pagar" se dice "Esa hora se llenó mientras revisabas. Elige otra" y se vuelve a "Cuándo", con todo lo demás intacto (0020, punto 7).
- **Si iniciar el pago falla** (Mercado Pago no responde), el pedido ya existe y se reintenta desde la misma pantalla: el backend ya permite reintentar un pago fallido.
- **El camino normal no pasa por aquí:** se pulsa "Pagar", se paga en Mercado Pago y Mercado Pago devuelve a nuestra página de resultado (aprobado, pendiente o rechazado).
- **Pantalla de rescate "Ya empezaste a pagar este pedido"** (decidida el 2026-11-19): aparece solo si la persona vuelve a nuestra pantalla de pago mientras su pago sigue abierto, por ejemplo con "atrás" desde Mercado Pago, o cerrando esa pestaña y entrando otra vez al carrito para pagar. El navegador recuerda que ese pedido ya tiene un pago empezado.
  - **Reemplaza por completo** a "Revisa y paga": no tiene "Cambiar" ni "Pagar", porque el pedido ya se creó. Así no se crea un segundo pedido ni se aparta cupo de más (pulsar "Pagar" otra vez respondería 409, "ya hay un pago en curso", ADR 0014).
  - **Texto:** título "Ya empezaste a pagar este pedido" y, debajo, "Si no lo terminaste, sigue en Mercado Pago. Si ya pagaste, lo confirmamos en unos minutos y te avisamos por correo." El título dice solo lo que sabemos: la persona pudo haber pagado y volver antes de que llegue el aviso de Mercado Pago, así que no se dice "te falta pagar" ni "tu pago está en proceso".
  - **Muestra** lo apartado (día, hora, quien recibe, total) con "Tu día y tu hora siguen apartados mientras pagas.", el botón "Terminar el pago en Mercado Pago" y el enlace "Ya pagué: ver mi pedido", que lleva a la página de resultado.
  - **El botón usa la dirección de Mercado Pago que guardó el navegador** al iniciar el pago, así que no hace falta cambiar el backend. Si ya venció (el cobro dura 15 minutos), se dice "El tiempo para pagar venció" y se ofrece empezar de nuevo. Recuperar la dirección desde el backend es la mejora del pendiente 32.

**5. El cupo se sigue apartando al crear el pedido, no antes.**
- Se evaluó apartarlo al elegir la hora (como Ocado, 60 minutos con cuenta, o los restaurantes en línea, 3 a 5 minutos). **No por ahora:** bloquearía cupo de gente que todavía no ha escrito sus datos, agrandaría el riesgo de apartados sin compra y exigiría un apartado sin pedido (tabla, proceso que lo libere y convertirlo en pedido), que cambia los ADR 0013, 0016 y 0020.
- **Se revisa** si en temporada se ve que se pierden ventas porque el cupo se va mientras la gente llena el último paso (pendiente 55).
- **Apartar sin comprar ya es posible hoy** (OWASP lo llama "denegación de inventario", OAT-021): crear un pedido como invitado aparta el cupo hasta 51 minutos. Las defensas están en el pendiente 54. Las dos primeras (un máximo de pedidos sin pagar por persona y un límite de pedidos por minuto en Nginx) entran con este cambio.

**6. Los datos de quien recibe son de otra persona.**
- Se usan solo para la entrega.
- El mensaje, los teléfonos y la nota **no se escriben en los logs**. Hoy el módulo de pedidos no escribe en los logs datos de quien compra; se mantiene así y se extiende a quien recibe.
- La política de tratamiento de datos (pendiente 53; Ley 1581, ya mencionada en el ADR 0003) tiene que cubrir también a quien recibe.

**7. Cambios en el backend.**
- **Migración V11.** La V10 ya está tomada en la rama de pagos (`feat/payment-foundations`), así que este cambio entra después de ella. En `customer_order`:
  - `recipient_name` (120) y `recipient_phone` (30), obligatorios. Los pedidos que ya existen se llenan con los datos de quien compra antes de marcarlos obligatorios, como se hizo en la V9.
  - `card_message` (hasta 1000) y `card_signature` (hasta 80), opcionales.
  - `delivery_notes` (hasta 200), opcional.

  En `order_settings`, `card_message_max_chars`, 250 por defecto, mayor que 0 y como máximo 1000.
- **`CreateOrderRequest`** suma quien recibe, el mensaje con su firma y la nota. El dominio del pedido, el mapper y sus pruebas cambian con ellos.
- **No cambian:** los estados del pago y del pedido, los bloqueos ni el orden pedido → pago → franja (ADR 0016).
- **La consola de la florista** (fase 4) muestra quien recibe, el mensaje con su firma y la nota en cada pedido.

## Detalles orientativos
- **Cómo se cuentan los caracteres:** igual en la pantalla y en el backend, para que el contador y la validación no se contradigan. Si ella imprime a mano, un emoji puede no tener sentido: confirmarlo con ella.
- **El carrito avisa** que en el último paso se puede agregar el mensaje (Baymard: que se vea desde el carrito que se puede mandar como regalo).
- **El correo de confirmación** repite quién recibe y el mensaje, para que quien compra lo revise. Cambiarlo después es por WhatsApp, mientras ella no haya empezado a armarlo.
- **Teléfono de quien recibe:** se recomienda decir debajo del campo "Solo lo usamos para coordinar la entrega".

## Pendiente
- **Con la florista:** el largo real de su tarjeta y si imprime o escribe a mano (emojis); si quiere un campo de "ocasión" para la tarjeta, que no se incluye por ahora.
- **Diseño del carrito y del último paso** con estas piezas (pendiente 52).
- **Defensas contra apartados sin compra** (pendiente 54).

## Consecuencias
- **A favor:**
  - El pedido refleja cómo se compran las flores: quien paga y quien recibe son distintos, y la tarjeta es parte del regalo.
  - La florista ajusta el largo del mensaje sin programar.
  - El último paso aparta el cupo lo más cerca posible de elegir la hora, sin apartados que bloqueen cupo de gente que no compra.
  - No cambian los estados, los bloqueos ni el momento en que se aparta el cupo.
- **En contra:**
  - El último paso pide más datos, y cada campo más es una razón más para abandonar. Por eso el mensaje y la nota son opcionales y "Soy yo" ahorra escribir.
  - Se guardan datos de una persona que no aceptó nada en la tienda: hay que cubrirlos en la política de datos.
  - El cambio espera a que entre la V10 de la rama de pagos.
  - Mientras no estén las defensas del pendiente 54, se puede apartar cupo sin comprar.

## Relación con otros ADR
| ADR | Relación |
|---|---|
| 0009 | El límite del mensaje se valida en el caso de uso, no en la base ni en el controlador |
| 0012 | Un pedido es una sola entrega: por eso un solo mensaje y una sola nota |
| 0013 | El pedido guarda la copia de lo que se pidió; el ajuste nuevo va en `order_settings`; el cupo se sigue apartando al crear el pedido |
| 0016 | No cambia estados, bloqueos ni el orden pedido → pago → franja |
| 0017 | La petición nueva llega al frontend por OpenAPI, como el resto de la API |
| 0020 | Precisa el orden del último paso (su punto 6) y usa su manejo del día que se vuelve viejo (punto 7) |
