# 0013. Pedidos: transacción con el cupo, copia de los datos, ajustes por módulo y tiempo de cupo apartado (enmienda del 0012)

**Estado:** aceptada, con valores por confirmar en el sandbox · **Fecha:** 2026-11-11

## Contexto
La fase 3 sigue con el módulo de pedidos (`ordering`): el cliente compra como invitado, elige una franja y, al empezar el pago, el cupo queda apartado (ADR 0012). Antes de programarlo quedaban cuatro decisiones técnicas abiertas (pendientes 5, 6, 17 y 26) y un valor sin fundamento: el ADR 0012 apartaba el cupo 15 minutos, un número que venía del plan y no de datos.

## Decisión

**1. Crear el pedido y apartar el cupo van en una sola transacción** (resuelve el pendiente 6).
- Si falla cualquiera de los dos pasos, no queda ninguno: ni un pedido sin cupo ni un cupo apartado sin pedido.
- Se reutiliza la reserva atómica que ya existe en `scheduling` (bloqueo de la fila de la franja, probada con 50 peticiones simultáneas), ahora con los cupos que ocupa el carrito.
- Como dice el ADR 0011, la transacción se declara en `infra/config` del módulo de pedidos, con una subclase del caso de uso que solo añade `@Transactional`. La reserva de `scheduling` se une a esa transacción.
- Un pedido tiene una sola franja, así que no hay riesgo de bloquear dos franjas en distinto orden.

**2. El pedido guarda una copia de lo que se compró, sin claves foráneas a otros módulos** (resuelve el pendiente 17).
- Cada línea guarda el id de la variante y una copia, en el momento de la compra, de: nombre del producto, nombre de la variante, precio unitario, horas de elaboración y cupos que ocupa.
- Si la florista cambia el precio o el tiempo mañana, los pedidos viejos no cambian, y el comprobante muestra lo que el cliente pagó.
- No hay claves foráneas de las tablas de pedidos hacia las de `catalog` ni `scheduling`. Se guarda el id de la variante y el de la franja como dato. La integridad la garantiza el caso de uso, que valida contra los servicios públicos de esos módulos antes de crear el pedido.
- Una prueba lee `information_schema` y falla si aparece una clave foránea entre tablas de módulos distintos.

**3. `shop_settings` se separa por módulo** (resuelve el pendiente 26).
- Cada módulo es dueño de sus ajustes (ADR 0001, regla 5): `scheduling` se queda con el margen de despacho y la capacidad por defecto de las franjas, y `ordering` con los máximos por línea y por pedido.
- Una migración crea las dos tablas nuevas, copia los valores actuales y elimina `shop_settings`. Cada una tiene una sola fila, como la original.

**4. Sin eventos internos por ahora** (resuelve el pendiente 5).
- El módulo de pedidos llama directo a los servicios públicos que necesita (`catalog` y `scheduling`). Es más simple de leer y de probar.
- Se vuelve a decidir cuando lleguen los correos (fase 6) o si los pagos lo exigen, y solo si una llamada directa crea un ciclo entre módulos o acopla de más.

**5. Cuánto se aparta el cupo: 36 minutos, por medio de pago y con fuentes** (enmienda el punto 9 del ADR 0012, que decía 15 minutos).
- **La preferencia de pago vence a los 15 minutos** de crearse (`expiration_date_to` de Checkout Pro). Coincide con el vencimiento que Mercado Pago documenta para PSE.
- **El cupo se aparta 36 minutos**: 15 minutos (el último momento en que el cliente puede iniciar el pago) + 21 minutos (lo máximo que tarda PSE en procesar una transacción). Es el peor caso documentado de un pago que sí se aprueba. Con tarjeta, el pago se resuelve casi siempre mucho antes.
- **Por medio de pago y en la base de datos**, como pedía el ADR 0003 ("cuánto tiempo se retiene el cupo"), editable sin desplegar.
- **Lo que queda fuera de la ventana** (pagos `in_process`, que pueden tardar hasta 2 días hábiles, o PSE que se pasa de los 21 minutos) no bloquea el cupo. Lo cubren las dos protecciones del ADR 0012: antes de liberar un cupo vencido se consulta al proveedor, y si el pago se aprueba después, el pedido queda "pagado y sin franja" y decide la florista.
- **Se ajusta con datos propios después de lanzar.** Cada pago guarda cuándo empezó y cuándo se aprobó, se rechazó o venció, por medio de pago. Con eso se mide cuánto tarda el 99 % de los pagos aprobados, y la retención se ajusta con ese número.

## Fuentes del punto 5
- Mercado Pago, PSE (Checkout API): "El pago creado con PSE expira automáticamente dentro de los 15 minutos de generado y su status pasa a ser rechazado." https://www.mercadopago.com.co/developers/es/docs/checkout-api-payments/integration-configuration/integrate-pse-avanza
- Mercado Pago, estados del pago: `in_process` se resuelve en "menos de 2 días hábiles"; un pago pendiente se cancela "luego de haber estado 30 días en un estado pendiente". https://www.mercadopago.com.co/developers/es/docs/checkout-api-payments/response-handling/query-results
- Mercado Pago, vigencia de la preferencia (Checkout Pro): `expires`, `expiration_date_from` y `expiration_date_to`; no documenta mínimo ni máximo. https://www.mercadopago.com.co/developers/es/docs/checkout-pro-preferences/additional-settings/term-of-preference
- En Mercado Pago Colombia, Nequi aparece como una de las entidades de PSE, no como un medio aparte (respuesta de ejemplo de la página de PSE citada arriba). Por eso tiene los mismos tiempos que PSE.
- PSE, tiempo máximo de procesamiento de 21 minutos y "el pago no se rechaza automáticamente, queda en estado pendiente": https://www.eltiempo.com/economia/finanzas-personales/pse-numero-de-pagos-que-se-pueden-hacer-al-dia-y-su-monto-maximo-852469 y https://www.tiendanube.com/blog/pse/
- Consultadas el 2026-11-11.

## Consecuencias
- **A favor:** no quedan pedidos ni cupos a medias; los pedidos viejos no cambian cuando cambia el catálogo; cada módulo es dueño de sus tablas y ajustes; el tiempo de cupo apartado tiene un fundamento citado y un camino para ajustarlo con datos reales.
- **En contra:** duplicar en el pedido los datos de la variante; una migración más para separar `shop_settings`; 36 minutos bloquean más un cupo que 15 cuando el cliente abandona la compra (se compensa liberando el cupo apenas el proveedor rechaza o vence el pago, sin esperar los 36 minutos).
- **Enmienda al 0012** solo en el tiempo de cupo apartado. El resto del 0012 no cambia.

## Pendientes
- **Confirmar en el sandbox** que el vencimiento de 15 minutos aplica también a Checkout Pro (la fuente es de Checkout API) y qué pasa si el cliente intenta pagar después de `expiration_date_to`.
- **Los 21 minutos** vienen de un banco y de prensa, no de ACH Colombia. Confirmarlos con la pasarela o en el sandbox.
- Medir después del lanzamiento cuánto tardan los pagos aprobados por medio, y ajustar la retención.
