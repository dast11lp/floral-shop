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

La organización de estas vistas (menú, reglas y pendientes) está explicada en `docs/diseno/organizacion-de-la-tienda.md`. El inicio (`04` y `05`) ya sigue esa organización.

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
