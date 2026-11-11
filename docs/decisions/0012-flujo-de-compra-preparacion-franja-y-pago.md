# 0012. Flujo de compra: tiempo de preparación, franja y pago (enmienda del 0003)

**Estado:** aceptada, con puntos pendientes de hablar con la florista; con una precisión de nombres (2026-11-11) · **Fecha:** 2026-11-10

## Contexto
Los productos no están hechos: se arman por encargo, y cada uno tarda distinto (un ramo grande para una boda no tarda lo mismo que uno pequeño). Hasta ahora el plan solo preveía una antelación mínima igual para todo (pendiente 20), que no refleja esa diferencia ni el tiempo de llevar el pedido. El negocio funciona desde una casa, sin tienda física, así que todo pedido se entrega a domicilio. Además, al empezar la fase 3 se revisó qué medios de pago convienen: los que tardan horas o días en confirmarse obligan a retener el cupo mucho tiempo y complican deshacer el pedido si el pago no llega.

## Decisión

**1. Flujo de compra, visto por el cliente.**
1. Ve los productos y los combos, y cada uno dice en cuánto tiempo puede estar listo.
2. Arma el carrito y elige su zona de entrega y su dirección.
3. La tienda calcula desde cuándo se puede entregar y le habilita solo las franjas que cumplen. Las demás se ven deshabilitadas.
4. Elige una franja y paga. Al empezar el pago, el cupo de esa franja queda apartado para el pedido.
5. Si el pago se aprueba, el pedido queda confirmado y la florista lo arma y lo entrega.
6. Si el pago se rechaza, se cancela o no llega a tiempo, se deshace todo: se libera el cupo, se cancela el pedido y se le avisa al cliente.

**2. Tiempo de preparación.**
- Se guarda **en horas, como número entero**: `product.preparation_hours` (obligatorio) y `product_variant.preparation_hours` (opcional). Si la variante no lo tiene, usa el del producto. Así la florista lo pone una sola vez cuando todas las variantes tardan lo mismo, y por variante cuando no.
- La API entrega las horas tal cual. El front decide cómo mostrarlas ("4 horas", "2 días").
- **Todos los días cuentan** por ahora, también los que ella no trabaja; esos días los maneja cerrando sus franjas.
- El tiempo es un mínimo: si ella termina antes, puede entregar antes.

**3. Tiempo de un carrito: se suma.** Ella trabaja sola y arma un producto después del otro, así que el carrito tarda la suma de cada línea por su cantidad: un ramo pequeño (4 h) más un ramo de boda (48 h) son 52 h, y 3 ramos pequeños son 12 h.
- La regla vive en **un solo servicio** que calcula "cuándo puede estar listo". Cambiarla después (que mande el más lento, sumar con descuento, contar solo días hábiles) es cambiar ese servicio y sus pruebas, sin tocar el catálogo, los pedidos ni la base de datos.
- Siguen aplicando los máximos por línea y por pedido de `shop_settings`, editables en la consola.

**4. Combos.** La florista arma combos en la consola juntando productos o variantes ("Combo boda: ramo grande + 2 centros de mesa").
- Para el cliente, un combo se ve y se compra como un producto más, con su precio y su tiempo.
- El sistema le **sugiere** el tiempo del combo sumando el de sus componentes, y ella lo puede cambiar (por ejemplo, si juntos los arma más rápido). En el carrito, un combo cuenta con el tiempo que ella dejó, no con la suma.
- **Se diseña desde la fase 3** (tablas, cálculo de tiempo y reserva), para que se pueda activar desde el primer día. Si se activa al lanzar o después lo decide ella.

**5. Márgenes de entrega.** Sobre el tiempo de preparación se suma el tiempo de llevar el pedido:
- **Margen general**, aparte del tiempo de preparación y sumado a él, con 2 horas de arranque. La florista lo edita desde la consola de administración; se guarda en `shop_settings`. Cubre empacar, salir y los imprevistos. Vive en la base de datos y no en el código, para que cambiarlo no exija compilar ni desplegar.
- **Margen por zona**, opcional, en `delivery_zone`: el tiempo extra de llegar a esa zona. Si una zona no lo tiene, no suma.
- Una franja se habilita si empieza después de: **ahora + preparación del carrito + margen general + margen de la zona**, tiene el cupo abierto y le caben los cupos del pedido.

**6. Zonas.** Una zona es un área con nombre que define la florista, con su costo de envío y su margen: puede ser un barrio, una localidad o un municipio cercano (Chía, por ejemplo), y ella las divide cuando lo necesite. El cliente elige su zona de una lista y escribe su dirección. Detectar la zona a partir de la dirección (con un servicio de mapas) queda para después y no cambia este modelo.

**7. Cupos que ocupa cada variante.** `product_variant.slot_units` (en la consola, "Cupos que ocupa"), con 1 por defecto. Un ramo que le cuesta a ella lo mismo que tres pequeños ocupa 3. Un pedido ocupa la suma de cantidad × cupos de cada línea, y la reserva atómica de la franja descuenta esa suma (`booked + :cupos <= capacity`). El tiempo decide **desde cuándo** se puede entregar; los cupos, **cuántos pedidos caben** en una franja.

**8. Medios de pago.**
- **Habilitados al lanzar:** tarjeta, PSE y Nequi, por la pasarela, porque responden en minutos.
- **Efecty:** fuera, porque tarda días en confirmarse.
- **Transferencia manual** (el cliente le transfiere directo a su Nequi o a su cuenta, y ella confirma en la consola que llegó): posible, **deshabilitada por defecto**. Se habilita si ella quiere, sabiendo que el pedido queda en espera hasta que lo confirme.
- **Contra entrega:** sigue deshabilitado por defecto (ADR 0003). Si se habilita, es con un monto máximo y solo para las variantes que ella marque como aptas (por ejemplo, las pequeñas).

**9. Cuánto se aparta el cupo y cómo se deshace.**
- El cupo se aparta durante un tiempo configurable (15 minutos de arranque para los medios en línea).
- **Antes de liberar un cupo vencido, se le pregunta al proveedor** (`fetchStatus`) si el pago ya se aprobó. Solo se libera si no.
- Un pago pasa a `EXPIRED` cuando el proveedor confirma que ya no se puede pagar, no solo porque venció el tiempo del cupo. Vencer el cupo es asunto del pedido; vencer el pago, del proveedor. La máquina de estados del pago no cambia.

**10. Pago aprobado cuando el cupo ya se liberó.** Es raro, pero posible (un banco que se demora más que el tiempo apartado y otro cliente que toma el cupo). El pago se registra como `PAID`, porque el dinero llegó. El pedido queda **pagado y sin franja**, se avisa a la florista y al cliente, y ella decide en cada caso: ofrecerle otra franja, devolverle el dinero o aceptarlo aunque se pase del cupo.

**11. Avisos y comprobante.**
- Al cliente se le avisa en la página y por correo, tanto si el pago se aprueba como si se deshace el pedido.
- El backend genera un **comprobante en PDF**. Se descarga desde la página de confirmación con un enlace que lleva un código que solo tiene el cliente (compra sin cuenta), y se adjunta al correo.
- El comprobante **no es una factura electrónica**. Eso sigue pendiente de revisar con un contador (ADR 0003), con cualquier medio de pago.
- El correo se adelanta a la fase 3 con un envío simple por el puerto `EmailSender`. La cola asíncrona (SQS a SES) sigue en la fase 6.

**12. Sedes en otras ciudades: no se construyen todavía.** Más adelante el negocio podría tener sedes (la misma tienda y la misma cuenta de cobro, con otra persona que arma y entrega en otra ciudad). Cada sede tendría sus zonas, franjas, márgenes y, quizá, sus tiempos. No se construye ahora (principio 5 del plan), pero las zonas, franjas y márgenes se diseñan como datos y no como constantes del código, para que agregar una sede después sea agregarles a qué sede pertenecen. Otras floristerías independientes usando la plataforma (multi-inquilino) quedan descartadas por ahora.

## Consecuencias
- **A favor:** el cliente ve fechas realistas antes de pagar; la florista controla sus tiempos, márgenes, zonas, combos y cupos sin programar; con medios que responden en minutos, deshacer un pedido es casi inmediato y el pago tardío tiene una salida definida; las reglas que pueden cambiar (cómo se suma el tiempo, días hábiles) viven en un solo lugar.
- **En contra:** quienes solo pueden pagar en efectivo no tienen opción al principio; el carrito necesita un cálculo de tiempo y de cupos que antes no existía; los combos y el correo suman trabajo a la fase 3; el cliente tiene que elegir zona antes de ver las franjas.
- **Reemplaza** al pendiente 20 (antelación mínima igual para todo).
- **Enmienda al 0003** en los medios habilitados al lanzar (Efecty fuera, transferencia manual deshabilitada por defecto) y en las condiciones del contra entrega. El resto del 0003 (`PaymentGateway`, registro por proveedor, idempotencia, validación del webhook) no cambia.

## Pendientes (hablar con la florista)
- **Combos:** si se activan al lanzar o después.
- **Zonas:** cuáles, con qué costo y qué margen (localidades, barrios o municipios cercanos).
- **Transferencia manual y contra entrega:** si habilita alguno, y con qué límites.
- **Valores de arranque:** las 2 horas de margen general y los 15 minutos de cupo apartado.
- Confirmar en el sandbox qué tan rápido responde Nequi por Mercado Pago, que según el 0003 pasaría por PSE (Wompi lo ofrece directo).

## Precisión de nombres (2026-11-11)
Al revisar el código, "preparation" y "margin" a secas no decían qué cubre cada tiempo. Se renombraron con la migración V4, sin cambiar datos ni reglas. El texto de arriba se deja como estaba.

| En este ADR | Nombre en el código | Qué cubre |
|---|---|---|
| Tiempo de preparación (`preparation_hours`) | `craftingHours` | Lo que tarda en elaborarse el producto o la variante |
| Margen general | `dispatchMarginHours` (margen de despacho) | Empacar, salir de la casa e imprevistos; aplica a todo pedido |
| Margen por zona | `travelMarginHours` (margen de viaje) | Lo extra de llegar a esa zona |

La fórmula queda: inicio más temprano = ahora + elaboración del carrito + margen de despacho + margen de viaje de la zona.
