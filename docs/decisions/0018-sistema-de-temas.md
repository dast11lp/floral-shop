# 0018. Sistema de temas: fondo, colores y banners que cambian por temporada (complementa el 0017)

**Estado:** aceptada, con detalles que se verifican al implementar · **Fecha:** 2026-11-16

## Contexto
La primera propuesta visual se va a rehacer (pendiente 40). Al buscar referencias salieron dos cosas:
- **Las fechas especiales venden:** San Valentín, Amor y Amistad, las flores amarillas del 21 de septiembre, el Día de la Madre y Navidad. Conviene que la tienda pueda cambiar de aspecto en cada temporada.
- **Un estilo no es solo un color de acento.** Algunas referencias que gustaron tienen el fondo oscuro o vino tinto, no blanco. Hasta ahora la idea era "fondo blanco fijo; solo cambia el acento". Eso se queda corto.

Hay dos límites:
- **Las fotos de producto.** Una tienda con fondo vino tinto se ve bien solo si las fotos se hicieron para ese fondo. La florista no puede repetir todas las fotos cada vez que cambia la temporada.
- **No hay ambientes.** Todo se desarrolla en local con Docker Compose, y el despliegue real llega al final, en la cuenta de ella (`docs/plan.md`). Nada de este ADR puede exigir un servidor aparte para probarse.

El ADR 0017 (punto 6) ya pide que ningún color vaya fijo en un componente. Este ADR dice qué es un tema, quién lo cambia y cómo llega a la página.

### Tema no es lo mismo que modo oscuro
- **Modo oscuro:** lo elige cada visitante en su celular. Sigue fuera por ahora.
- **Tema:** lo elige la tienda, y todos los visitantes ven el mismo.

### Cómo leer este documento
- **Decisión:** lo aprobado el 2026-11-16, una decisión por vez.
- **Detalles orientativos:** recomendaciones sin aprobar. Se confirman al implementar.
- **Pendiente:** lo que falta decidir.

## Decisión

**1. Un tema es un juego completo, no un solo color.**

| Cambia con el tema | No cambia nunca |
|---|---|
| Fondo, tarjetas y bordes | La organización de las páginas |
| Color de los textos y del acento | Las letras (punto 7) |
| La versión del logo: para fondo claro o para fondo oscuro | Las formas: esquinas redondeadas y botones (punto 7) |
| Los banners y las fotos de campaña | Las fotos de producto (punto 3) |

**2. Se empieza con el tema claro de base.**
- Es el primero que se construye y el que se usa todo el año.
- Los demás son de temporada. Cuando termina su fecha, la tienda vuelve sola al tema claro.
- La paleta exacta del tema claro todavía no está decidida (ver Pendiente).

**3. Las fotos de producto van siempre en fondo neutro.**
- Así sirven para cualquier tema sin repetirlas.
- En un tema de fondo oscuro, la foto va dentro de su tarjeta redondeada, con su fondo neutro. Se ve como algo hecho a propósito, no como un recorte pegado.
- Lo que sí se hace para cada tema son **los banners y las fotos de campaña**: son pocas, y es donde está la emoción de la temporada.

**4. Temas prediseñados, por el momento.**
- Los temas se diseñan y se revisan antes de ofrecerlos. La florista elige entre ellos; no arma colores sueltos.
- **Por qué:** nunca se rompe el diseño ni la legibilidad, y cada tema se prueba una sola vez.
- **Alternativa descartada por ahora:** colores libres. Da más libertad, pero puede quedar ilegible, y el sistema tendría que bloquear las combinaciones malas. Se puede sumar después sin cambiar el resto.

**5. Se activa a mano o se programa por fechas.**
- Desde la consola (fase 4), la florista puede cambiar el tema cuando quiera.
- También puede programarlo, por ejemplo "vino tinto del 1 al 14 de febrero".
- Al terminar la fecha programada, vuelve solo al tema claro. Así no depende de acordarse de quitarlo.
- **Las fechas programadas no se pueden cruzar:** la consola no deja guardar una programación que se superponga con otra.
- **El tema tiene que existir:** si llega el nombre de un tema que no existe, se rechaza y el tema activo se queda como está.

**6. Con vista previa.**
- Antes de activar o programar un tema, la florista ve la tienda completa con ese tema.
- Solo ella puede abrir esa vista. Los clientes siguen viendo el tema activo.

**7. Las letras y las formas no cambian con el tema.**
- La tienda se reconoce en cualquier temporada: cambia de ropa, no de cara.
- Los temas son más fáciles de diseñar y de probar, y no se cargan letras extra en el celular.

**8. Selector de temas, solo en el modo demo.**
- El proyecto es también una demostración. En modo demo aparece un botón flotante para cambiar de tema en vivo mientras se recorre la tienda.
- **El modo demo no es un ambiente:** es un interruptor de configuración de la misma aplicación. Se prueba gratis en local.
- En producción está apagado: ahí el tema solo lo cambia la florista (puntos 5 y 6).

**9. Cada tema tiene que pasar la prueba de contraste.**
- Una prueba automática en el CI revisa todos los temas con el estándar WCAG AA: 4,5 a 1 para el texto normal; 3 a 1 para el texto grande, los bordes de los campos y los íconos.
- Si un tema no se lee bien, el CI falla y el tema no se puede usar.

## Detalles orientativos (recomendaciones sin aprobar; se confirman al implementar)
- **Cómo se define un tema:** un grupo de variables CSS (fondo, superficie, texto, texto secundario, acento, texto sobre el acento, borde) bajo un atributo en la etiqueta `<html>`, por ejemplo `data-theme="claro"`. Es la forma en que Tailwind con shadcn/ui ya trabaja con colores (ADR 0017, punto 6). Cambiar de tema es cambiar ese nombre.
- **Dónde viven los temas:** los colores de cada tema viven en el código del frontend, en un solo archivo. El backend guarda el **nombre** del tema activo y la programación, y es el dueño de la **lista de nombres válidos**: rechaza cualquier otro (punto 5).
- **Una sola lista de nombres:** el backend publica esa lista en su contrato OpenAPI, y el frontend genera de ahí sus tipos (ADR 0017, punto 4). Si se agrega un nombre en el backend y el frontend no tiene sus colores, el frontend no compila. Así las dos listas no se desalinean.
- **Por si acaso:** como el frontend y el backend se despliegan por separado (ADR 0007), puede haber un momento en que el backend ya conozca un tema nuevo y el frontend todavía no. Mientras tanto, el frontend muestra el tema claro en lugar de romperse.
- **Sin parpadeo:** el servidor de Next.js pone el tema en el HTML que envía. Así no se ve "primero blanco y después vino tinto".
- **Cuánto tarda en verse un cambio:** las páginas públicas se generan y se guardan (ADR 0017, punto 3). Al cambiar el tema hay que regenerarlas:
  - a mano: el backend avisa a Next.js para que las regenere;
  - por fechas: las páginas se regeneran cada pocos minutos, para que el inicio y el fin programados se noten solos.
  - "En vivo" significa segundos o pocos minutos, no al instante. Para una tienda alcanza.
- **Fechas en hora de Bogotá:** la programación se guarda y se compara en la hora de Colombia, para que "del 1 al 14 de febrero" no empiece ni termine cinco horas antes.
- **Interruptor del modo demo:** se lee al arrancar el servidor, no al construir la imagen. Con `NEXT_PUBLIC_*` quedaría fijo en la imagen y rompería "una imagen para todos los ambientes" (ADR 0007).
- **Selector de la demo:** cambia el tema solo en el navegador de quien lo usa, sin pasar por el backend ni cambiar lo que ven los demás. Puede recordar la elección en `localStorage`.
- **Vista previa:** una ruta o un parámetro que solo abre una sesión de administración, sin caché.
- **Logo:** en SVG, con su versión para fondo claro y para fondo oscuro.
- **Colores de terceros:** los botones de la pasarela de pago y el de WhatsApp conservan sus colores de marca en cualquier tema.

## Pendiente
- **Qué temas forman el primer juego** además del claro (por ejemplo, vino tinto para San Valentín, Navidad y amarillo de septiembre), y **la paleta exacta** de cada uno, incluido el claro. Se decide con la identidad visual, al terminar el descubrimiento de diseño, en otro ADR.
- **Lo que necesita el backend (fase 4):** guardar el tema activo, la programación por fechas (sin cruces, punto 5) y los banners de cada campaña, con su parte en la consola.
- **Una fecha secundaria dentro de una principal:** por ejemplo, un tema largo de temporada con un tema corto encima, unos días en medio. Hoy no se ve en qué época del año haría falta, así que no se permite (punto 5). Si aparece el caso, se decide entonces cuál manda y a cuál se vuelve al terminar el corto.
- **Banners:** dependen de cómo se sirven las fotos (ADR 0017, Pendiente). En la demostración se usan fotos con licencia libre; nada real va al repositorio (ADR 0002).
- **Logo en dos versiones:** pedírselo a la florista junto con su logo conceptual.
- **Correos de campaña:** si usan los colores del tema activo. Se decide cuando se hagan.
- **Más adelante, sin fecha:** colores libres (punto 4) y modo oscuro.

## Consecuencias
- **A favor:**
  - La tienda puede vestirse para cada temporada sin tocar código ni repetir las fotos.
  - La florista no puede romper el diseño: los temas son prediseñados, se prueban en el CI y se ven antes de activarse.
  - La demostración luce: se cambia de tema en vivo, en local y sin costo.
  - Agregar un tema es agregar un grupo de colores y su banner, no rehacer pantallas.
- **En contra:**
  - Cada tema hay que diseñarlo y revisarlo; la florista no lo hace sola.
  - Los cambios tardan unos minutos en verse, por la caché de las páginas públicas.
  - Las fotos de producto quedan atadas a un fondo neutro: no se puede hacer una tienda "toda vino tinto", con las fotos hechas en ese escenario, como en algunas referencias.
  - El backend suma trabajo en la fase 4: tema activo, fechas y banners.

## Relación con otros ADR
| ADR | Relación |
|---|---|
| 0002 | Repositorio público: las fotos reales de campañas no van al repositorio |
| 0007 | "Una imagen para todos los ambientes": el interruptor del modo demo se lee al arrancar, no al construir |
| 0015 | Las páginas públicas se arman en el servidor: el tema llega puesto en el HTML |
| 0017 | Lo complementa: el punto 6 pide colores en variables; este ADR dice cómo se agrupan en temas, quién los cambia y cómo se regeneran las páginas guardadas (punto 3) |
