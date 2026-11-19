# Propuesta de diseño 2 (2026-11-17)

Maquetas de la segunda propuesta visual, la que reemplaza a la primera (`docs/diseno/propuesta-1`). Las decisiones están en el ADR 0019, y los valores exactos en `docs/diseno/guia-de-estilo.md`.

**No es código del proyecto** y no se usa para construir el frontend: es material de referencia. El frontend real se construye con Next.js, Tailwind y shadcn/ui (ADR 0017), siguiendo la guía de estilo.

## Cómo verlas
Descarga el archivo y ábrelo con doble clic: se ve en cualquier navegador, sin instalar nada. GitHub muestra el código de un HTML, no la página.

Se necesita internet para dos cosas: las letras (Google Fonts) y las fotos (Unsplash).

## Qué hay

| Archivo | Qué es | Estado |
|---|---|---|
| `01-direccion-a-limpia.html` | Dirección A, limpia. Inicio en celular | Descartada |
| `02-direccion-b-suave.html` | Dirección B, suave. Inicio en celular | Base de la mezcla final |
| `03-direccion-c-elegante.html` | Dirección C, elegante. Inicio en celular | Se tomaron la foto grande arriba y los pasos |
| `04-inicio-celular.html` | Inicio final en celular, ajustado a la organización: tipos, Destacados y "Momentos y sentimientos" | **Aprobado** |
| `05-inicio-escritorio.html` | Inicio final en escritorio, con el mismo ajuste | **Aprobado** |
| `06-tablero-de-estilo.html` | Tablero de estilo: letras, colores, formas, degradados, botones, fotos e íconos | **Aprobado**, ajustable por un diseñador (ADR 0019, punto 8) |
| `07-catalogo-celular.html` / `08-catalogo-escritorio.html` | Catálogo: todos los arreglos, filtro por tipo | **Aprobado** |
| `09-ocasion-celular.html` / `10-ocasion-escritorio.html` | Una ocasión (ejemplo: Cumpleaños), con su encabezado | **Aprobado** |
| `11-ocasiones-celular.html` / `12-ocasiones-escritorio.html` | Índice de ocasiones: temporada, Momentos, Sentimientos y calendario de fechas especiales | **Aprobado** |
| `13-menu-celular.html` / `14-menu-escritorio.html` | Menú abierto, con las ocasiones en dos grupos | **Aprobado** |
| `15-producto-celular.html` / `16-producto-escritorio.html` | Página del producto completa, con la entrega del ADR 0020 (carrito vacío y zona elegida) | **Aprobado** (2026-11-18) |
| `17-producto-calendario-escritorio.html` | Escritorio: "Más" abre el calendario en una ventanita junto a la fila de días | **Aprobado** (2026-11-18) |
| `18-entrega-sin-zona-celular.html` | Recuadro de entrega sin zona: "Este ramo llegaría desde mañana" | **Aprobado** (2026-11-18); los textos se pulen al construir (ADR 0020) |
| `19-entrega-con-carrito-celular.html` | Con productos en el carrito, el ramo no mueve el día: una sola frase | **Aprobado** (2026-11-18) |
| `20-entrega-mueve-el-dia-celular.html` | El ramo mueve el día: aviso en rosa, "Sin él, sería…" y "Agregar y pasar al viernes 20" | **Aprobado** (2026-11-18) |
| `21-entrega-calendario-celular.html` | Celular: "Más" abre el calendario desde abajo | **Aprobado** (2026-11-18) |
| `22-entrega-fecha-lejana-celular.html` | Eligió una fecha lejana: queda marcada en el botón del final de la fila | **Aprobado** (2026-11-18) |
| `23-carrito-al-agregar-celular.html` | Al agregar: la persona se queda en el producto, el botón dice "Agregado ✓" y aparece un aviso pequeño con "Ver carrito" y "Seguir mirando" | **Aprobado** (2026-11-19) |
| `24-carrito-celular.html` | Carrito con zona: entrega, productos con − cantidad +, aviso de regalo, resumen y barra del total fija abajo | **Aprobado** (2026-11-19) |
| `25-carrito-sin-zona-celular.html` | Carrito sin zona: "Según tu zona" y "Total sin envío" | **Aprobado** (2026-11-19) |
| `26-carrito-deshacer-celular.html` | Quitó un producto: aviso "Deshacer" encima de la barra fija | **Aprobado** (2026-11-19) |
| `27-carrito-algo-cambio-celular.html` | Algo cambió desde que se guardó: precio nuevo, cantidad ajustada al máximo, tamaño que ya no está y día que se movió | **Aprobado** (2026-11-19) |
| `28-carrito-vacio-celular.html` | Carrito vacío, con "Ver el catálogo" y Destacados | **Aprobado** (2026-11-19) |
| `29-carrito-escritorio.html` | Carrito en escritorio: productos a la izquierda y tarjeta quieta a la derecha | **Aprobado** (2026-11-19) |

La organización de estas vistas (menú, reglas y pendientes) está explicada en `docs/diseno/organizacion-de-la-tienda.md`. El inicio (`04` y `05`) ya sigue esa organización.

Las reglas del producto y de su recuadro de entrega (qué se dice en cada caso, cuándo se vuelve a consultar, el calendario y el tope de días) están en el ADR 0020. Las maquetas 18 a 22 muestran solo la parte de arriba del producto, con cada estado del recuadro. Las fechas, zonas y precios son de ejemplo. La foto principal cambia con el tamaño (pendiente 42); en una maqueta quieta no se ve.

Las decisiones del carrito están en el pendiente 52. **Lo que se mueve está marcado en el HTML** con un comentario que empieza por `COMPORTAMIENTO:` y un atributo `data-comportamiento`:
- `fija-abajo`: la barra del total del carrito en celular (`position: sticky; bottom: 0`). El contenido se desliza por debajo. Se ve fija al abrir la maqueta en una ventana del alto de un celular.
- `fija-arriba`: la tarjeta del resumen en escritorio (`position: sticky; top: 24px`).
- `aviso-temporal`: los avisos "Agregado" y "Deshacer", que se van solos a los pocos segundos. En la maqueta están quietos; en la tienda van fijos a la pantalla.

Las tres direcciones (01 a 03) se guardan como historia: muestran de dónde salió la mezcla final. Están tal como se compararon, incluidos sus defectos. Por ejemplo, en la C la última ocasión queda cortada.

## Fotos
- **Ninguna foto está guardada en este repositorio** (ADR 0002): cada imagen se carga desde `images.unsplash.com`.
- Son fotos de ejemplo, con la [licencia de Unsplash](https://unsplash.com/license), que permite usarlas gratis y sin pedir permiso.
- En la tienda real irán las fotos de la florista.
- Si Unsplash retira una foto, en su lugar se verá un recuadro vacío. El diseño sigue intacto.
- En el tablero de estilo, la foto de los pétalos con grano se muestra a propósito como **ejemplo de lo que no hay que usar**.

## Qué es marcador
El nombre "Valentía", el lema "Regala con valentía", los productos, los precios, el teléfono y el correo son de ejemplo (ADR 0002). El nombre y el lema los decide la florista.

## De dónde salen
Se diseñaron en un lienzo privado y se convirtieron a HTML normal, sin el programa del lienzo. Si se ajusta el diseño, la fuente de verdad es la guía de estilo, no estos archivos.
