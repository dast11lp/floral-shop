# 0019. Identidad visual: letras, colores, formas y fotos de la tienda (complementa el 0018)

**Estado:** aceptada, ajustable por un diseñador (punto 8) · **Fecha:** 2026-11-17

## Contexto
La primera propuesta visual se veía rígida, fría y de lujo anticuado, y había que rehacerla (pendiente 40). Se rehízo por descubrimiento, decidiendo con imágenes y no con adjetivos, porque las palabras se prestaban a confusión: lo que para uno era "juvenil", para otro era "de viejo".

**Qué se busca, en imágenes:**
- **Sí:** limpio, luminoso, con aire, pasteles (rosa empolvado, lila), formas redondeadas, una serifa moderna de alto contraste y las fotos como protagonistas. Referencias que gustaron: Kilo de Flor, Bloomy, Vivienne y el concepto "Flowers".
- **No:** lo llamativo "Gen Z" (colores chillones, letras gruesas) ni el lujo clásico de las grandes floristerías en línea.

**Cómo se llegó:**
- Tres direcciones de la misma pantalla de inicio, lado a lado: A limpia, B suave y C elegante.
- Se descartó la A. Se mezclaron la B (pasteles, letra Gloock) y la C (foto grande arriba, pasos con números grandes), y se sumó el collage de categorías de la A.
- Se afinaron las tarjetas, los botones y los degradados comparando versiones, y apoyándose en fuentes de usabilidad: Nielsen Norman Group, Baymard Institute y WebAIM.
- El inicio quedó aprobado en celular y en escritorio el 2026-11-17.

**Hoy no hay un diseñador profesional en el proyecto.** Las decisiones las tomó quien vende, comparando opciones. Por eso este ADR deja dicho qué se puede afinar sin pedir permiso y qué no (punto 8).

El ADR 0018 dejó pendiente "la paleta exacta del tema claro". Este ADR la fija, junto con las letras y las formas que el 0018 declara fijas en todos los temas.

### Cómo leer este documento
- **Decisión:** lo aprobado el 2026-11-17, una decisión por vez.
- **Detalles orientativos:** recomendaciones sin aprobar. Se confirman al implementar.
- **Pendiente:** lo que falta decidir.
- **Los valores exactos** (códigos de color, tamaños en píxeles, medidas de los degradados) están en `docs/diseno/guia-de-estilo.md`. Este ADR dice qué se decidió y por qué; la guía dice cuánto mide cada cosa.
- **Las maquetas** están en `docs/diseno/propuesta-2`: las tres direcciones que se compararon, el inicio aprobado en celular y en escritorio, y el tablero de estilo. Se abren con doble clic en cualquier navegador.

## Decisión

**1. Dirección: suave y editorial.**
- Fondo blanco, pasteles rosa y lila en bloques y tarjetas, malva como acento y fotos grandes.
- Una sola acción principal por zona, y mucho aire alrededor.

**2. Dos familias de letras, las dos gratuitas (licencia OFL).**

| Familia | Para qué | Por qué |
|---|---|---|
| **Gloock** | Títulos, nombres de producto y números grandes | Serifa moderna de alto contraste, parecida a las de las referencias que gustaron, sin ser de lujo clásico |
| **Manrope** | Textos, botones, menús y precios | Sin serifa, clara y legible en tamaños pequeños |

- No se usan otras familias. Cormorant Garamond, la de la primera propuesta, queda descartada.
- Gloock va solo en peso normal: nunca en negrita ni en cursiva.
- En escritorio, el título de la portada va siempre en dos renglones, por ancha que sea la pantalla: "Regala emociones" y debajo "con valentía". En celular se acomoda al ancho.

**3. Paleta del tema claro.**

| Color | Para qué |
|---|---|
| Tinta (ciruela casi negro) | Títulos, textos e íconos |
| Tinta suave | Textos secundarios |
| Malva | Botón principal, enlaces, números y detalles |
| Malva profundo | Textos de acento que pueden quedar encima de una foto, como "con valentía" |
| Rosa empolvado y lila | Fondos de tarjetas, bloques y franjas |
| Borde | Píldoras y líneas divisorias |
| Blanco | Fondo de la página |

- Todos los textos pasan WCAG AA (4,5 a 1). El cálculo de cada par está en la guía.
- Es la paleta del tema claro. Los temas de temporada (ADR 0018) cambian estos colores; las letras y las formas no.

**4. Formas: redondo, pero no infantil.**
- **Esquinas:** una sola medida, 22 px, para tarjetas, fotos y franjas. *Precisión del 2026-11-19 (ajuste fino, punto de "Ajustes finos"):* lo que va de borde a borde de la ventana (las franjas de color, el panel derecho del último paso) va con esquinas rectas; los 22 px son para lo que vive dentro del contenido (detalle en la guía de estilo).
- **Píldoras:** botones, ocasiones y etiquetas, con bordes totalmente redondos.
- **Espacios:** siempre múltiplos de 4.
- **Ancho del contenido:** máximo 1200 px, centrado. La foto de portada y las franjas de color van de borde a borde.
- **Lo que se toca** mide al menos 44 px.

**5. Degradados: solo para fundir fotos.**
- La portada y los banners se funden en blanco, y el texto va donde el fondo ya es casi blanco.
- Las tarjetas de producto se funden en rosa o lila, con un degradado largo y suave.
- **Nunca en botones.** El botón principal es malva plano: el degradado se probó y se veía más pesado que el resto de la página.
- **Sin efecto de vidrio fuerte** (desenfoque marcado y bordes brillantes): se veía "hecho con IA".
- **El contraste se mide en el punto más débil** del degradado, no en el promedio (WebAIM).

**6. Botones, enlaces y tarjetas.**
- **Un solo botón principal por zona.** En la portada, "Ver el catálogo"; "Cómo funciona" sobraba, porque esa sección ya está más abajo en la página.
- **Los botones parecen botones.** Nada de botones fantasma (solo borde) ni enlaces sueltos para la acción principal, porque la gente duda de si se pueden tocar (Nielsen Norman Group).
- **Enlaces** de "ver más": malva en negrita, con flecha.
- **Píldoras con borde:** solo para filtros y ocasiones.
- **Tarjetas de producto sin botón de agregar.** Toda la tarjeta lleva al producto: foto completa, nombre en Gloock y precio.
  - **Por qué:** cada arreglo pide elegir tamaño o presupuesto, día, zona y franja antes de ir al carrito (ADR 0012). Un "+" en la tarjeta no podría agregar nada; solo abriría el producto.
  - Baymard también encuentra que en tiendas generales ese botón suele sobrar.

**7. Fotos: son las protagonistas.**
- **Nítidas,** sin grano ni filtros, con luz natural y tonos rosa, blanco o lila. Una foto con grano se ve de mala calidad en pantalla grande, aunque sea linda y tenga buena resolución.
- **Producto sobre fondo neutro** (ADR 0018, punto 3).
- **Tamaño mínimo:** el doble de lo que se muestra en pantalla.
- **Mismo encuadre en todo el catálogo:** vertical 3:4 y el arreglo centrado.

**8. Es una base, ajustable por un diseñador.**
Si más adelante entra un diseñador profesional, puede afinar esta identidad sin rehacer la tienda. El ADR 0017 (punto 6) ya pide que los colores vivan en variables. Este ADR extiende esa regla a las letras, los tamaños, las esquinas y los espacios: ninguno va fijo en un componente.
- **Ajustes finos: no necesitan ADR nuevo.** Por ejemplo un tono, un tamaño de letra, un espacio, la medida de las esquinas o el largo de un degradado.
  - Se cambian en la guía de estilo y en las variables del frontend.
  - Se anotan en la sección "Cambios" de la guía.
  - Deben pasar la prueba de contraste (ADR 0018, punto 9) y respetar los puntos 1 a 7.
- **Cambios de fondo: sí necesitan un ADR nuevo que reemplace a este.** Por ejemplo otra familia de letras, otra dirección (dejar los pasteles, pasar a fondo oscuro), volver a poner botones en las tarjetas o usar degradados en los botones.
- **Por qué esta línea:** los valores se eligieron comparando opciones, sin un profesional. Afinarlos debe ser fácil. Pero cambiar la dirección sin dejar registro haría que las pantallas se separen unas de otras.

## Detalles orientativos (recomendaciones sin aprobar; se confirman al implementar)
- **Variables:** cada color de la guía tiene un nombre de variable (`--tinta`, `--acento`, `--pastel-rosa`...). Al implementar se hacen corresponder con los nombres que usa shadcn/ui (`--foreground`, `--primary`, `--muted`, `--border`...), dentro del grupo del tema claro (ADR 0018).
- **Letras:** se cargan con `next/font`, que las descarga al construir y las sirve desde la misma tienda, sin pedirlas a Google en cada visita. Solo los pesos que se usan: Manrope 400, 500 y 600, y Gloock 400.
- **Tamaños que se adaptan:** los títulos pasan del tamaño de celular al de escritorio según el ancho de la pantalla. La guía da los dos extremos.
- **Movimiento:** en escritorio, la foto de una tarjeta se acerca un poco al pasar el mouse. Si la persona pidió reducir el movimiento en su equipo, no se mueve.
- **WhatsApp:** botón flotante discreto, en un círculo blanco con el ícono verde, que conserva su color en cualquier tema (ADR 0018).

## Pendiente
- **Las demás pantallas** (catálogo, producto, carrito, checkout y resultado del pago): se rehacen con estas reglas en la etapa 5 del rediseño, en celular y en escritorio.
- **Paletas de los temas de temporada** (ADR 0018, Pendiente): se diseñan con las mismas reglas de contraste.
- **Logo:** lo define la florista, en versión para fondo claro y para fondo oscuro. "Valentía" y "Regala con valentía" son provisionales: el nombre y el lema los decide ella.
- **Fotos reales:** cuando estén las fotos de la florista, revisar con ellas el degradado de las tarjetas y el contraste del texto.
- **Categorías y ocasiones** (pendiente 39): el inicio muestra las dos, las ocasiones en píldoras y las categorías en el collage, así que la decisión de modelo sigue abierta.
- **Revisión por un diseñador profesional,** cuando sea posible (punto 8).
- **Más adelante, sin fecha:** modo oscuro (ADR 0018).

## Consecuencias
- **A favor:**
  - Todas las pantallas salen de las mismas reglas, en lugar de copiar a ojo la pantalla de inicio.
  - Las decisiones tienen sus razones escritas: usabilidad, contraste medido y las referencias que gustaron.
  - Un diseñador puede afinar valores sin pedir un ADR, y la tienda se ajusta sin rehacer componentes.
  - Las dos letras son gratuitas y se pueden usar en un repositorio público.
- **En contra:**
  - La identidad no pasó por un diseñador profesional. Puede necesitar ajustes cuando llegue uno.
  - Se apoya en fotos nítidas y en buena luz. Con fotos malas, el estilo pierde mucho.
  - Gloock tiene un solo peso. Si algún día se quiere una serifa en negrita, hay que cambiar de familia, y eso es un cambio de fondo (punto 8).

## Relación con otros ADR
| ADR | Relación |
|---|---|
| 0002 | Repositorio público: ninguna foto se guarda en el repositorio (las maquetas cargan las de ejemplo desde Unsplash); las letras tienen licencia libre |
| 0012 | Cada arreglo pide variante, día, zona y franja: por eso las tarjetas no llevan botón de agregar (punto 6) |
| 0017 | Los colores son variables (punto 6); este ADR extiende la regla a letras, tamaños, esquinas y espacios, para que un diseñador pueda afinarlos sin rehacer componentes |
| 0018 | Lo complementa: fija la paleta del tema claro y las letras y formas que el 0018 declara fijas en todos los temas |
