# Guía de estilo de la tienda

**Documento vivo.** Aquí están los valores exactos de la identidad visual: códigos de color, tamaños y medidas. Las decisiones y sus razones están en el ADR 0019.

**Un diseñador puede ajustar esta guía** sin escribir un ADR nuevo, siempre que:
- el cambio sea fino, como un tono, un tamaño, un espacio, la medida de las esquinas o el largo de un degradado;
- pase la prueba de contraste (ADR 0018, punto 9);
- respete las decisiones del ADR 0019 (puntos 1 a 7).

Cada ajuste se anota en la sección **Cambios**, al final. Los cambios de fondo, como otra familia de letras u otra dirección, necesitan un ADR nuevo (ADR 0019, punto 8).

Esta guía describe el **tema claro**, el de todo el año. Los temas de temporada cambian los colores; las letras, los tamaños, las formas y las reglas de fotos son iguales en todos (ADR 0018).

## 1. Letras

| Familia | Uso | Pesos | Licencia |
|---|---|---|---|
| Gloock | Títulos, nombres de producto y números grandes | 400 (el único que tiene) | OFL |
| Manrope | Textos, botones, menús y precios | 400, 500 y 600 | OFL |

| Uso | Familia | Escritorio | Celular | Notas |
|---|---|---|---|---|
| Título de portada | Gloock | 72 px | 42 px | Interlineado 1,02 a 1,06. En escritorio, siempre en dos renglones: "Regala emociones" / "con valentía". En celular se acomoda al ancho |
| Título de página (Catálogo, Ocasiones) | Gloock | 56 px | 36 px | |
| Título del encabezado de una ocasión | Gloock | 52 px | 32 px | Por ejemplo "Flores para cumpleaños" |
| Título de sección | Gloock | 40 px (34 en el índice de ocasiones) | 28 px (26 en el índice) | |
| Ruta ("Ocasiones / Cumpleaños") | Manrope 400 | 14 px | 13 px | Color tinta suave; el primer tramo es enlace |
| Número de paso | Gloock | 48 px | 32 px | En malva |
| Nombre de producto | Gloock | 22 px | 17 px | |
| Etiqueta del collage del inicio | Gloock | 22 / 20 px | 18 / 16 px | Grande / pequeña |
| Texto | Manrope 400 | 16 px | 15 px | Interlineado 1,5 |
| Subtítulo de portada | Manrope 400 | 18 px | 15 px | |
| Botón y menú | Manrope 500 | 16 px | 15 px | |
| Precio y notas | Manrope 400 | 15 px | 13 px | |
| Etiqueta pequeña | Manrope 600 | 12 px | 12 px | Mayúsculas, espaciado de 1,4 px |

## 2. Colores del tema claro

Contraste calculado con la fórmula de WCAG 2.1. El mínimo es 4,5 a 1 para texto normal, y 3 a 1 para texto grande e íconos.

| Nombre | Variable | Código | Uso | Contraste |
|---|---|---|---|---|
| Tinta | `--tinta` | `#2E2433` | Títulos, textos principales e íconos | 14,8 a 1 sobre blanco |
| Tinta suave | `--tinta-suave` | `#62566A` | Textos secundarios | 6,9 sobre blanco; 6,1 sobre lila |
| Tinta de tarjeta | `--tinta-tarjeta` | `#4A3F50` | Precio sobre el degradado de las tarjetas | 9,2 sobre rosa |
| Malva | `--acento` | `#7A5C9E` | Botón principal, enlaces, números y detalles | Texto blanco encima: 5,4; malva sobre lila: 4,8 |
| Malva profundo | `--acento-profundo` | `#5E4580` | Textos de acento que pueden quedar sobre una foto ("con valentía") | 7,9 sobre blanco |
| Rosa empolvado | `--pastel-rosa` | `#FCF4F2` | Fondos de tarjetas y bloques | Solo fondo |
| Lila | `--pastel-lila` | `#F6EEF7` | Fondos de tarjetas, franjas y botones suaves | Solo fondo |
| Borde | `--borde` | `#E4D8E8` | Borde de píldoras | Decorativo |
| Línea | `--linea` | `#E2D4E5` | Líneas divisorias | Decorativo |
| Blanco | `--fondo` | `#FFFFFF` | Fondo de la página | Base |
| WhatsApp | `--whatsapp` | `#1E7A4C` | Solo el ícono de WhatsApp | 5,3 sobre blanco |
| Aviso | `--aviso` | `#9A4F5E` | Texto del aviso suave del recuadro de entrega ("Sin él, sería el jueves 19.") | 5,3 sobre rosa empolvado; 5,7 sobre blanco |
| Borde de aviso | `--borde-aviso` | `#EBD3D0` | Borde del recuadro de entrega cuando avisa | Decorativo |

## 3. Formas y espacios

| Elemento | Valor |
|---|---|
| Esquinas de tarjetas, fotos, collage y franjas | 22 px |
| Botones, píldoras y etiquetas | Totalmente redondos (la mitad de su alto) |
| Escala de espacios | 4, 8, 12, 16, 24, 32, 48, 64, 72, 88 px |
| Ancho máximo del contenido | 1200 px, centrado |
| Margen lateral | 16 px en celular; 24 px o más en escritorio |
| Separación entre tarjetas | 14 px en celular; 24 px en escritorio |
| Alto mínimo de lo que se toca | 44 px |
| Botón principal | 48 px en celular; 52 px en escritorio |

## 4. Degradados

| Uso | Valor |
|---|---|
| Portada, escritorio | `linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.9) 40%, #FFF 72%)` sobre la parte de abajo de la foto |
| Portada, celular | `linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.92) 32%, #FFF 62%)` |
| Tarjeta de producto | Del color de la tarjeta (rosa o lila) transparente a sólido, en 8 pasos: 0, 0,06, 0,2, 0,42, 0,68, 0,88, 0,97 y 1, cada 14 % |

Las tarjetas de producto alternan rosa y lila.

Prohibido, por el ADR 0019:
- degradados en botones;
- efecto de vidrio fuerte;
- degradados entre colores lejanos.

## 5. Botones, enlaces y tarjetas

| Pieza | Cómo se ve |
|---|---|
| Botón principal | Fondo malva plano, texto blanco en Manrope 500, píldora. Uno por zona |
| Enlace "ver más" | Malva, Manrope 600, con flecha (→) |
| Píldora de filtro (tipo) | Borde de 1 px color borde, fondo blanco, Manrope 500. **Elegida:** rellena en color tinta, texto blanco y una "×" para quitarla. Sin píldora "Todos": sin filtro elegido se ve todo. Con un filtro elegido aparece el enlace "Quitar filtros" |
| Encabezado de una ocasión | Foto a la izquierda (5/12 en escritorio; arriba, 170 px, en celular) y panel rosa empolvado con el título y una o dos líneas. Esquinas de 22 px. Bajito, unos 300 px en escritorio, para que los productos se vean en la primera pantalla. Nunca carrusel ni con cara de anuncio |
| Foto de ocasión (índice, menú y "Otras ocasiones") | Foto con una etiqueta blanca en Gloock y un círculo malva con flecha, abajo a la izquierda; en el índice, además, una frase corta. En el menú de celular, miniatura redonda de 40 a 44 px |
| Destacado de temporada | Como el encabezado de una ocasión, con "Temporada · ahora" en malva y un botón principal ("Ver flores para mamá") |
| Fecha del calendario | Tarjeta con borde: mes abreviado en Gloock malva, nombre y cuándo cae. **La activa:** fondo malva, texto blanco y "Disponible ahora →". En celular, filas en lugar de tarjetas |
| Barra de avance de "Ver más" | 3 px de alto, fondo lila y avance en malva, sobre el botón, con el texto "Ves 8 de 24 arreglos" |
| Tarjeta de producto | Foto que llena la tarjeta en 3:4, degradado abajo, nombre en Gloock y "Desde $precio". Sin botón: toda la tarjeta es el enlace |
| Collage del inicio ("Momentos y sentimientos") | Una foto grande y dos pequeñas, con la misma pieza que la foto de ocasión: etiqueta blanca en Gloock y círculo malva con flecha, **en las tres** |
| Píldora de tamaño | Como la de filtro, más alta (58 a 60 px), con el nombre en Manrope 600 y el precio debajo en 12 a 13 px. La elegida, rellena en tinta. Al cambiarla cambian el precio, la foto principal y la línea del reloj (ADR 0020) |
| Línea del reloj | Ícono de reloj malva y una línea: lo que tarda en Manrope 600 y la explicación en tinta suave ("En tamaño Pequeño, lo armamos en 4 horas. Si llevas varios productos, los tiempos se suman.") |
| Recuadro de entrega | Fondo lila, esquinas de 22 px. Arriba, camión malva con "Entrega en Chapinero" en tinta suave y "Cambia tu zona" en malva a la derecha; sin zona, el botón blanco "Elige tu zona". Una sola frase en 17 a 18 px con el día en negrita. Va encima del botón principal (ADR 0020) |
| Recuadro de entrega con aviso | El mismo recuadro con fondo rosa empolvado, borde de aviso y, debajo de la frase, el aviso en 13 px ("Sin él, sería el jueves 19."). Solo cuando el ramo mueve el día |
| Día de la fila | Píldora de 62 × 58 px con el día corto arriba (12 px) y el número abajo (16 px). **Elegido:** relleno tinta. **Cerrado o sin cupo:** borde punteado, texto apagado `#A99DB0` y tachado. La fila se desvanece a la derecha para mostrar que se desliza |
| Botón "Más" | Al final de la fila, fijo: ícono de calendario y "Más" en malva, fondo blanco. **Con una fecha lejana elegida:** relleno tinta con el día ("Jue 3 dic"). **Abierto:** fondo lila y borde malva de 2 px |
| Calendario | Celular: hoja que sube desde abajo sobre un velo tinta al 45 %. Escritorio: ventanita blanca de 380 px junto a "Más", con borde y sombra suave. Título "Elige el día" en Gloock, el límite en una línea, mes con flechas, días de 40 a 44 px: elegido en tinta, cerrados o sin cupo tachados, después del límite muy claros (`#D3C9D8`; como están deshabilitados, WCAG no les exige contraste); debajo, "Los días tachados están cerrados o sin cupo." y el botón principal "Elegir el jueves 3 de diciembre" |
| Aviso al agregar | Recuadro blanco con borde y sombra suave, abajo de la pantalla: foto pequeña (48 × 60 px), "Agregado al carrito" en tinta suave y el producto en Manrope 600; dos botones del mismo alto, "Ver carrito" (principal) y "Seguir mirando" (borde). Se va solo. El botón de agregar pasa un momento a relleno tinta con "Agregado ✓" |
| Producto en el carrito | Foto 3:4 de 84 px (110 en escritorio), esquinas de 16 px; nombre en Manrope 600 y total de la línea a la derecha; debajo "Tamaño Pequeño · $120.000 c/u" en tinta suave; selector de cantidad en píldora con borde (− número +, botones de 44 px) y "Quitar" a la derecha. Avisos de la línea en color aviso (13 px). Si el tamaño ya no está, la foto y el nombre se apagan |
| Aviso "Deshacer" | Píldora de 22 px de esquinas, fondo tinta, texto blanco ("Quitaste Tulipanes envueltos") y "Deshacer" en Manrope 600. Encima de la barra del total; se va solo |
| Aviso de regalo | Recuadro lila con ícono de regalo malva: "En el siguiente paso dices **quién recibe** y escribes el **mensaje de la tarjeta**." |
| Resumen del pedido | Filas en tinta suave (Subtotal, "Envío a Chapinero") y el total en Manrope 600, 18 px, con una línea arriba. Sin zona: "Según tu zona · Elige tu zona" y "Total sin envío" |
| Barra del total (celular) | **Fija abajo** (`position: sticky; bottom: 0`): fondo blanco, línea arriba y sombra suave hacia arriba; a la izquierda "Total" y el valor, a la derecha "Continuar". El contenido se desliza por debajo. Apagada (malva claro `#C9BCCF`) cuando algo impide seguir, con la razón encima en color aviso |
| Pasos de "Así funciona" | Franja lila de borde a borde. Número grande en Gloock malva, título en Manrope 600 y una línea divisoria. En escritorio, los tres pasos van uno al lado del otro |

## 6. Fotos
- Nítidas, sin grano ni filtros, con luz natural y tonos rosa, blanco o lila.
- Producto sobre fondo neutro (blanco o gris claro).
- Encuadre vertical 3:4 con el arreglo centrado y aire alrededor.
- Tamaño mínimo: el doble de lo que se muestra. Portada: 2400 px de ancho o más. Tarjetas: 1000 px o más.
- Antes de usar una foto, mirarla en grande: el grano de un filtro no se arregla con más resolución.

## 7. Íconos y movimiento
- Íconos de línea, trazo de 1,5 px, a 24 px, en color tinta.
- WhatsApp: botón flotante de 52 a 56 px, círculo blanco con sombra suave e ícono verde.
- En escritorio, la foto de una tarjeta se acerca un 3 % al pasar el mouse, en medio segundo. Sin movimiento si el equipo pide reducirlo.

## Cambios
| Fecha | Cambio | Quién |
|---|---|---|
| 2026-11-17 | Primera versión, a partir del inicio aprobado en celular y escritorio | Equipo del proyecto |
| 2026-11-17 | Corrección: los dos renglones fijos del título de portada son solo para escritorio; en celular se acomoda al ancho | Equipo del proyecto |
| 2026-11-17 | Piezas nuevas del catálogo y de las ocasiones: título de página, ruta, píldora de filtro elegida, encabezado de ocasión, foto de ocasión, destacado de temporada, fecha del calendario y barra de avance | Equipo del proyecto |
| 2026-11-17 | Inicio ajustado: el collage pasa a ser de ocasiones ("Momentos y sentimientos") y todas sus fotos llevan flecha; la etiqueta pequeña sube a 20 px en escritorio | Equipo del proyecto |
| 2026-11-18 | Página del producto aprobada (ADR 0020): colores de aviso y piezas nuevas (píldora de tamaño, línea del reloj, recuadro de entrega, día de la fila, botón "Más" y calendario) | Equipo del proyecto |
| 2026-11-19 | Carrito aprobado: aviso al agregar, producto en el carrito, aviso "Deshacer", aviso de regalo, resumen y barra del total fija abajo | Equipo del proyecto |
