# Organización de la tienda: menú y vistas

**Documento vivo.** Explica cómo está organizada la tienda: qué hay en el menú, qué vistas existen y cómo se conectan. Las letras, los colores y las formas están en `docs/diseno/guia-de-estilo.md` (ADR 0019). Las maquetas están en `docs/diseno/propuesta-2`.

Estado al 2026-11-17: el inicio, el menú, el catálogo, la página de una ocasión y el índice de ocasiones están **aprobados**.

## Las reglas
1. **El menú lleva a lugares. Los filtros ordenan lo que hay dentro de un lugar.** Nunca se mezclan.
2. **Dos puertas, un solo mostrador.** "Catálogo" y "Ocasiones" son dos entradas distintas en el menú. Las dos terminan en la misma rejilla de productos, con la misma tarjeta y el mismo "Ver más".
3. **Cada idea tiene siempre la misma forma:**
   - las **ocasiones** se muestran **con foto**, porque son emocionales;
   - los **tipos** se muestran **en píldoras**, porque son prácticos y sirven para filtrar.
4. **Las mismas palabras en todas partes.** "Cumpleaños" se llama igual en el menú, en los títulos y en la ruta ("Ocasiones / Cumpleaños").
5. **Ningún paso obligatorio de más.** Desde el menú se llega a cada ocasión con un clic. El índice de ocasiones es un camino opcional. Baymard encontró que en tiendas pequeñas las páginas intermedias confunden: el 31 % de las personas se enredó para llegar a los productos.

## Tres clases de "¿para qué es?"
| Clase | Qué es | Ejemplos | Cuándo se ve |
|---|---|---|---|
| **Momentos** | Algo que pasa en la vida de alguien, cualquier día del año | Cumpleaños, Aniversario, Nacimiento, Condolencias | Todo el año |
| **Sentimientos** | Lo que se siente por alguien, sin fecha | Amor, Gratitud, Perdón | Todo el año |
| **Fechas especiales** | Un día del calendario, el mismo para todos | San Valentín, Día de la Mujer, Día de la Madre, Día del Padre, Amor y Amistad, 21 de septiembre, Navidad | **Solo en su temporada** |

- Momentos y Sentimientos van **separados** en el menú y en el índice, con su título cada uno.
- **Por qué "Sentimientos"** (antes "Para decir"): Amor, Gratitud y Perdón no son ocasiones por sí solas, y con un título que era media frase parecían colados. "Sentimientos" es un nombre que hace pareja con "Momentos": una ocasión puede ser **algo que pasa** o **algo que sientes**. Los nombres van como sustantivos, por eso "Gratitud" y no "Gracias".
- Una fecha especial es una ocasión **con fecha de inicio y de fin**. Usa la misma programación de temporadas del ADR 0018: aparece sola, con su banner y su tema, y desaparece sola al terminar.
- Las ocasiones y fechas de esta tabla son ejemplos. Las reales las decide la florista.

## Menú
```
Catálogo · Ocasiones ▾ · [fecha especial, solo en temporada] · Zonas de entrega · Cómo funciona
```
- **Escritorio:** "Ocasiones" abre un panel con dos grupos con foto, **Momentos** y **Sentimientos**, y el enlace "Ver todas las ocasiones".
- **Celular:** el botón de menú abre un cajón con "Catálogo" grande arriba, las ocasiones a la vista en sus dos grupos (con miniatura), "Ver todas las ocasiones", Zonas de entrega, Cómo funciona y WhatsApp.
- El nombre se queda en **"Catálogo"** (decidido). Baymard considera que una etiqueta genérica agrega un paso, pero en escritorio la acepta para tiendas pequeñas, y en celular las ocasiones quedan a la vista.

## Vistas

| Vista | Qué tiene | Maquetas |
|---|---|---|
| **Inicio** | Portada (en temporada, la campaña de la fecha). Debajo, píldoras de **tipo** (Ramos, En florero, Cajas) que abren el catálogo filtrado. **Destacados**: 4 productos que elige la florista. **Momentos y sentimientos**: una foto grande de Cumpleaños y dos pequeñas (Aniversario y Amor), con "Ver todas las ocasiones →". Así funciona y pie | `04`, `05` |
| **Catálogo** | Título y texto, **sin foto**. Un solo filtro: **tipo** (Ramos, En florero, Cajas), sin "Todos". Contador, rejilla, "Ver más arreglos" con barra de avance y bloque de WhatsApp | `07`, `08` |
| **Una ocasión** | Ruta "Ocasiones / Cumpleaños". **Encabezado editorial compacto**: foto de la ocasión y panel pastel con "Flores para cumpleaños" y dos líneas propias. Luego **la misma** rejilla y el mismo filtro por tipo. Al final, "Otras ocasiones" | `09`, `10` |
| **Índice de ocasiones** | Título; **destacado de temporada** si hay una activa (si no, la próxima fecha); **Momentos** y **Sentimientos** con fotos; **calendario de fechas especiales** (solo la activa lleva enlace, las demás dicen "Próximamente"); bloque de WhatsApp. **Sin rejilla de productos** | `11`, `12` |
| **Menú abierto** | Ver "Menú" arriba | `13`, `14` |

- **Lo que cambia entre catálogo y ocasión es solo el encabezado.** Se investigó que Kilo de Flor usa la misma página con otro título, e Interflora la misma página con un encabezado compacto.
- **El encabezado nunca parece un anuncio.** Nada de carruseles, letras de oferta ni pantallas completas. La gente ignora lo que parece publicidad (Nielsen Norman Group), y Baymard vio que un banner sobre la lista se confunde con su título.
- **En temporada**, el encabezado de la fecha especial usa el banner de la campaña (ADR 0018).
- **En el inicio, la temporada vive solo en la portada.** La foto grande de "Momentos y sentimientos" es siempre Cumpleaños, para no repetir la fecha especial dos veces seguidas. En temporada, la florista puede poner además en Destacados los arreglos de la fecha.
- **"Destacados" y no "Lo más vendido"**, porque por ahora los elige la florista y el título tiene que ser verdad (pendiente 45). Interflora hace lo mismo con "Nuestra selección".
- **Por qué el inicio quedó así:** Nielsen Norman Group compara el inicio con la vitrina de una tienda: decir quién eres, mostrar lo principal y dejar empezar a comprar, sin amontonar. Baymard encontró que la gente deduce qué tan amplia es la tienda por lo que ve en el inicio. Los tres tipos y tres ocasiones con foto muestran bastante sin llenarlo. Kilo de Flor e Interflora siguen el mismo orden: accesos por tipo u ocasión, y después productos elegidos.
- **Filtro de tipo dentro de una ocasión:** permite, por ejemplo, "Ramos para cumpleaños". La elegida va rellena en color tinta y con una "×" para quitarla.

## Pendiente
- **Lo que necesita el backend:** está en `docs/pendientes.md` (pendientes 39 y 42 a 45).
- **Confirmar con la florista** qué ocasiones, motivos y fechas especiales usa de verdad, y cómo ordena sus productos.
- **Pantallas que faltan:** carrito, checkout y resultado del pago. El producto se aprobó el 2026-11-18 (ADR 0020; maquetas 15 a 22 de `propuesta-2`).
