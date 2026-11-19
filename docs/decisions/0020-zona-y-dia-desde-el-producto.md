# 0020. Zona y día desde el producto (enmienda del 0012, punto 1)

**Estado:** propuesta, borrador para revisión · **Fecha:** 2026-11-18

## Contexto
El ADR 0012 (punto 1) deja la zona, la dirección y la franja para después del carrito. El cliente ve en el producto cuánto tarda en armarse ("Lo armamos en 4 horas"), pero no sabe **qué día le llegaría** hasta el último paso. Y en una floristería la fecha es casi siempre lo más importante: se compra para un cumpleaños, un aniversario o "para mañana".

Además, el tiempo de un carrito se suma (ADR 0012, punto 3). Agregar un ramo puede mover el día de entrega de todo lo que ya se lleva, y con el flujo del 0012 el cliente se entera recién al final.

**Qué dicen los expertos** (revisado el 2026-11-18):
- **Nielsen Norman Group:** dejar que el cliente indique su ubicación desde el producto. Si no la da, mostrar un mínimo o un rango ("desde"), sin obligarlo a dar datos personales. Para pocas fechas posibles, una lista; para fechas lejanas, otra forma de elegirlas.
- **Baymard Institute:** la estimación va cerca del botón de comprar, dice para qué lugar es y se puede cambiar ahí mismo. Mostrar una **fecha**, no una velocidad, porque el cliente la lee como una **promesa**: tiene que ser exacta. No pedir dos veces lo mismo: lo que se eligió antes llega puesto.
- **Lo que viene marcado por defecto** se lee como una recomendación.
- **En los mercados en línea,** el día apartado vence, y cambiar el carrito lo puede romper. Por eso aquí nada se aparta antes de pagar.

**Cómo se llegó:** se dibujaron varias rondas en el lienzo de diseño, comparando textos y casos (carrito vacío, carrito con productos, el ramo que mueve el día y el último paso). La página del producto y los estados del recuadro de entrega se aprobaron el 2026-11-18: están en `docs/diseno/propuesta-2`, maquetas 15 a 22. El último paso no está aprobado: se diseña con el checkout.

### Cómo leer este documento
- **Decisión:** lo acordado el 2026-11-18, una decisión por vez.
- **Detalles orientativos:** recomendaciones sin aprobar. Se confirman al construir la pantalla.
- **Pendiente:** lo que falta decidir.
- Las fechas, zonas y precios de los ejemplos son inventados.

## Decisión

**1. El flujo de compra cambia así** (reemplaza el punto 1 del 0012):
1. En el producto, el cliente ve **qué día le llegaría**. Puede elegir su zona ahí mismo, pero **no está obligado**. Sigue viendo también cuánto tarda en armarse (la línea del reloj, como en el 0012).
2. Si elige su zona, ve los días y puede marcar uno. Si ya lleva productos, ve cómo queda el día de todo el carrito con este ramo.
3. Arma el carrito. La zona y el día se recuerdan.
4. En el último paso, la zona y el día **llegan puestos** y **se pueden cambiar**. Ahí escribe su dirección, elige la hora y paga.
5. Desde ahí, todo sigue igual que en el 0012 (puntos 1.4 a 1.6) y el 0013: el día y la hora se apartan **al crear el pedido**, cuando el cliente pulsa "Continuar al pago", no antes.

**2. Elegir la zona y el día en el producto no aparta nada.** Es una consulta. El cupo se aparta al crear el pedido, como hasta ahora (0013). Lo que se ve antes puede quedar viejo; el punto 7 dice cómo se maneja.

**3. Qué ve el cliente en el producto.** El recuadro va encima del botón de agregar:

| Caso | Qué dice |
|---|---|
| Carrito vacío, sin zona | "Este ramo llegaría desde **mañana**." y, debajo, "El día exacto depende de tu zona.", con el botón "Elige tu zona" |
| Carrito vacío, con zona | "Entrega en Chapinero · Cambia tu zona"; "Este ramo llegaría **mañana, miércoles 18**." y la fila de días con el más pronto ya marcado |
| Ya lleva productos y este ramo no mueve el día | Una sola frase: "Con este ramo, todo puede llegar el **jueves 19**." |
| Ya lleva productos y este ramo mueve el día | El recuadro pasa a rosa: "Con este ramo, todo puede llegar el **viernes 20**." y, debajo, "Sin él, sería el jueves 19."; el botón dice "Agregar y pasar al viernes 20", y se ofrece "O elige un tamaño que llegue el jueves" |

- **Sin zona, se calcula con la zona de mayor margen de viaje.** Así el día que se muestra se cumple en cualquier zona; si el cliente está más cerca, puede ser antes.
- **El enlace dice "Cambia tu zona",** no "Cambiar" a secas: tiene que decir qué cambia (también por accesibilidad, WCAG 2.4.4).

**4. Cómo se dice, según el momento.**
- **"Llegaría"** mientras solo se mira el ramo: todavía no está en el carrito.
- **"Puede llegar"** con productos en el carrito y hasta continuar al pago: el día no está apartado.
- **"Llega"** solo después de pagar, cuando el pedido está confirmado.
- **Una sola frase si nada cambia.** Dos textos solo cuando el ramo mueve el día.
- **No se dice "tu pedido":** se presta a pensar que es solo ese ramo. Se dice "todo" o "este ramo".

**5. Los días: una fila corta y un calendario.**
- La fila muestra los próximos días, con el más pronto ya marcado. Hoy, si ya no alcanza, se ve apagado.
- Al final de la fila hay un botón fijo, **"Más"**, que abre un calendario del mes hasta el último día que se puede pedir (punto 8).
- En los dos, los días **cerrados o sin cupo se ven tachados**, y los que pasan del límite, muy claros.
- Si el cliente elige una fecha lejana en el calendario, ese botón la muestra marcada ("Jue 3 dic") y la frase cambia: "Este ramo llegaría el **jueves 3 de diciembre**."
- La misma fila y el mismo calendario se usan en el último paso.

**6. El último paso llega con todo puesto y se puede cambiar.**
- La zona y el día vienen de lo elegido antes, y se pueden cambiar. Cada cambio vuelve a consultar al backend.
- Si cambiar la zona mueve el día, se dice con la misma forma: "Con Chía, todo puede llegar el **viernes 20**. Con Chapinero, sería el jueves 19."
- Si el día elegido ya no sirve (se llenó o el carrito cambió), se marca el primer día que sí sirve y se avisa con la misma frase.
- Debajo del botón: "Apartamos tu día y tu hora al continuar al pago."

**7. Lo que se ve puede quedar viejo: se vuelve a consultar y se avisa.** Entre ver el producto y pagar pueden pasar minutos o días (el carrito se queda guardado). En ese tiempo el día que se mostró puede dejar de servir. No se vende un cupo de más, porque al crear el pedido se revisa todo otra vez con la franja bloqueada; el cuidado está en la pantalla.
- **Por qué cambia,** de lo más común a lo más raro:
  - pasa el tiempo y se cruza la hora de corte de una franja (ramo de 4 horas más 2 de despacho: la franja de hoy de 2 a 6 deja de servir a las 8:00 de la mañana);
  - el carrito se guardó de un día para otro y el día elegido ya pasó;
  - otros clientes llenan el cupo (común en el Día de la Madre y en Amor y Amistad);
  - un cupo se ve lleno mientras otra persona paga y se libera si no termina (51 minutos como máximo, ADR 0013);
  - la florista cierra una franja o cambia el cupo o el tope de días.
- **La fecha se guarda exacta** (2026-11-18), nunca como "mañana". Al volver, si ya pasó o no sirve, se marca el primer día que sí sirve.
- **Se vuelve a consultar:** al cambiar de tamaño (variante); al volver a la pestaña; antes de agregar al carrito (el botón "Agregar y pasar al viernes 20" tiene que ser cierto); al entrar al último paso; y cuando se cruza la hora de corte del día que se muestra, que el navegador calcula con lo que ya recibió, sin preguntar a cada rato.
- **Al cambiar de tamaño:** la foto principal, el precio y la línea del reloj cambian sin consultar, porque ya vienen con el producto. El recuadro de entrega sí se consulta de nuevo: cada variante tarda distinto (`craftingHours`) y ocupa distintos cupos en la franja (`slot_units`), así que pueden cambiar el día más pronto y los días con lugar.
- **Si cambia, se avisa con la misma forma de siempre:** "Ya no alcanzamos para el miércoles 18. Lo más pronto ahora es el jueves 19." Sin errores ni textos nuevos.
- Esto ya pasaba con el 0012 entre ver las franjas y pagar; con este ADR la ventana es más larga, por eso se escribe la regla.

**8. Cuántos días hacia adelante se puede pedir.**
- El límite real es el último día con franjas que abrió la florista.
- Además hay un tope, que hoy está fijo en el código (31 días, `DateRange.MAX_DAYS`). Pasa a ser un **ajuste de la tienda** en `delivery_settings`, editable desde la consola, con **30 días** de arranque.
- Así ella puede abrir con más tiempo las fechas especiales (Día de la Madre, Amor y Amistad) sin tocar código.
- **No se deja abierto** (pedidos a un año): cambian los precios y las flores de temporada, ella puede no trabajar ese día, y el dinero queda guardado mucho tiempo si toca devolverlo.

**9. Cambios en el backend.** Casi todo ya existe:
- `POST /api/v1/delivery-options` ya calcula los días con la zona y las líneas, sin guardar ni apartar nada. El frontend lo llama de nuevo cuando cambia la zona, el tamaño o el carrito.
- Para "Sin él, sería…", el frontend lo llama dos veces: con el carrito y con el carrito más este ramo.
- **Lo nuevo:**
  - Aceptar la consulta **sin zona** (`zoneId` opcional) y calcular con la zona de mayor margen de viaje. Cambian la petición, el servicio que calcula la fecha y sus pruebas.
  - El tope de días como ajuste (punto 8): una columna en `delivery_settings` con su migración, y el servicio que lo lee en lugar de la constante.
- El pedido no cambia: `CreateOrderService` ya recalcula las opciones y valida la franja con la zona y las líneas que llegan.

**10. Cambios en el frontend.**
- La zona y el día elegidos se guardan junto con el carrito, en el navegador (ADR 0017, punto 2). Solo se guarda la zona, no la dirección.
- La página del producto se genera en el servidor y se guarda (ADR 0017, punto 3). Pero el recuadro de entrega depende de la hora, del carrito y de la zona del cliente, así que **se llena en el navegador** con RTK Query. Mientras carga, muestra un espacio reservado del mismo tamaño, para que la página no salte.

## Detalles orientativos
- **Cuántos días muestra la fila:** de 7 a 10. Se ajusta al construirla.
- **Cuidar las conexiones a la base:** el navegador guarda la respuesta mientras no cambien el carrito ni la zona (RTK Query), consulta solo con el recuadro a la vista y, si hace falta, Nginx limita las consultas por persona.
- **El "Sin él, sería…" con dos consultas:** si algún día pesa, se puede pedir al backend las dos fechas en una sola respuesta. No hace falta para empezar.
- **Las fotos cambian con el tamaño** (pendiente 42): cada variante necesita su foto principal.

## Pendiente
- **La línea del reloj** ("En tamaño Pequeño, lo armamos en 4 horas. Si llevas varios productos, los tiempos se suman.") **se mantiene** junto al recuadro de entrega (decidido el 2026-11-18). No cuesta una consulta más: las horas de armado ya vienen con el producto. Cuando todo el sistema esté construido, se evalúa si sobra al lado del día de entrega (pendiente 50).
- **Pulir los textos** con la pantalla completa. La idea está acordada; las palabras exactas pueden cambiar.
- **El tope de días** (30 de arranque): confirmarlo con la florista (pendiente 48).
- **La plantilla semanal de franjas** (pendiente 49): con 30 días hacia adelante, crear las franjas una por una no escala.
- **Versión de escritorio** del recuadro de entrega y del calendario.
- **Mensaje de la tarjeta:** no existe en el sistema. Se ve con el carrito.

## Consecuencias
- **A favor:**
  - El cliente sabe desde el producto qué día le llegaría, sin dar datos personales.
  - Ve antes de agregar si un ramo mueve el día de todo lo que lleva, y puede elegir otro tamaño.
  - No se le pregunta dos veces la zona ni el día.
  - El backend casi no cambia, y lo que cambia es aditivo: la API acepta lo de antes.
  - La florista controla hasta cuándo se puede pedir sin programar.
- **En contra:**
  - La página del producto hace una o dos consultas más al backend por cada visita con el recuadro visible.
  - Lo que se ve en el producto no es una reserva: el día puede llenarse antes de pagar, y el último paso tiene que manejarlo con claridad.
  - Sin zona, la fecha es la de la zona más lejana, y alguien cercano puede ver un día más tarde del real.
  - Más estados que diseñar y probar en la pantalla del producto.
  - Más lecturas a la base. No bloquean nada, pero en días de mucho tráfico ocupan conexiones, y el aviso de Mercado Pago no debe quedarse esperando una (pendiente 36).

## Relación con otros ADR
| ADR | Relación |
|---|---|
| 0012 | Reemplaza el punto 1 (flujo de compra) y quita la consecuencia "el cliente tiene que elegir zona antes de ver las franjas". El resto (tiempos que se suman, márgenes, zonas, cupos, medios de pago) no cambia |
| 0013 | Sin cambios: el cupo se aparta al pagar, con los mismos tiempos |
| 0016 | No agrega bloqueos ni cambia el orden pedido → pago → franja. El producto y el último paso solo leen, y en PostgreSQL una lectura no espera a un bloqueo; apartar la franja sigue igual |
| 0017 | El recuadro de entrega se llena en el navegador, aunque la página del producto se genere en el servidor; la zona y el día se guardan con el carrito |
| 0019 | Usa su identidad (colores, píldoras, recuadros de 22 px). El aviso usa el rosa empolvado y dos colores nuevos, aviso y borde de aviso, sumados a la guía de estilo el 2026-11-18 |
