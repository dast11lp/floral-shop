# 0017. Arquitectura del frontend: estado, datos, contrato con la API, carpetas e interfaz (complementa el 0015)

**Estado:** aceptada, con detalles que se verifican al implementar · **Fecha:** 2026-11-16

## Contexto
El ADR 0015 eligió Next.js con React y TypeScript, y dijo qué se renderiza en el servidor y qué en el navegador. No dijo **cómo** se construye: dónde vive el estado, cómo se traen los datos, cómo se evita que el frontend y el backend se desalineen, cómo se ordenan las carpetas ni con qué se hace la interfaz.

Al decidirlo pesaron cuatro cosas:
- **Experiencia previa:** React con Vite y Redux Toolkit. En Next.js el estado global no se maneja igual.
- **El público:** gente común, sobre todo desde el celular, y no los celulares más potentes.
- **El servidor:** pequeño, con poco tráfico al principio.
- **El objetivo:** una demostración que parezca producción.

### Cómo leer este documento
- **Decisión:** lo aprobado el 2026-11-16, una decisión por vez.
- **Detalles orientativos:** recomendaciones sin aprobar. Se confirman al implementar.
- **Pendiente:** lo que falta decidir.

## Decisión

**1. Cada tipo de estado vive en su lugar.** En Next.js no conviene un solo almacén global con todo. El estado se reparte así:

| Tipo de estado | Ejemplo | Dónde vive |
|---|---|---|
| Datos del servidor | productos, franjas, opciones de pago | En las páginas de servidor, o en la caché de RTK Query (punto 3) |
| Estado en la URL | categoría, página | Los parámetros de la URL |
| Estado del cliente | carrito, lo elegido en el checkout | Redux Toolkit (punto 2) |
| Formularios | datos del cliente, dirección | React Hook Form con Zod (punto 6) |

**2. Estado del cliente: Redux Toolkit.**
- Guarda el carrito y lo que el cliente va eligiendo en el checkout (zona, dirección, franja y opción de pago).
- **No guarda** productos ni franjas: esos son datos del servidor (punto 3).
- En Next.js el almacén no es una variable global: se crea con una función, uno por petición en el servidor y uno solo en el navegador. Un componente de cliente lo entrega al resto de la aplicación.
- El carrito se guarda en el navegador (ADR 0014, punto 3).
- **Por qué:** ya se domina, y aparece en muchas ofertas de trabajo.
- **Alternativas descartadas:**
  - Zustand: más liviano y con menos código. Era la recomendación inicial; se prefirió Redux Toolkit por la experiencia previa.
  - Context con `useReducer`: sin dependencias, pero vuelve a pintar todo lo que lo consume y escala mal.

**3. Datos del servidor: páginas de servidor para lo público, RTK Query para el navegador.**
- **Inicio, catálogo y producto** se arman en el servidor (Server Components con `fetch`), como pide el ADR 0015 por el SEO. Se generan y se guardan: no se renderizan en cada visita, sino cada cierto tiempo o cuando cambia un producto. Así el servidor casi no trabaja.
- **En el navegador** (franjas según el carrito y la zona, opciones de pago, estado del pago y el panel de administración) se usa RTK Query, que viene con Redux Toolkit.
- **Por qué:** con Redux ya elegido, RTK Query evita sumar otra librería con otra forma de pensar. Mismo almacén y mismas herramientas de depuración.
- **Alternativas descartadas:** TanStack Query (la más completa, pero serían dos librerías) y SWR (se queda corta para el panel).

**4. Contrato con la API: tipos y hooks generados desde el OpenAPI del backend.**
- El backend ya publica su contrato con springdoc. Una herramienta oficial (`@rtk-query/codegen-openapi`) lo lee y genera los tipos de TypeScript y los hooks de RTK Query.
- El contrato se exporta a un archivo JSON, que se guarda en el repositorio.
- **El CI falla si el contrato cambió y no se regeneró.** Es la prueba de contrato que pide el ADR 0007.
- Las páginas de servidor no pueden usar hooks: importan solo los tipos generados y llaman al backend con un `fetch` pequeño y tipado.
- Los errores del backend llegan en un formato estándar (`ProblemDetail`). Se traducen a mensajes para el cliente en un solo lugar.
- El archivo generado no se edita a mano. Lo que haya que ajustar va en un archivo aparte.
- **Alternativas descartadas:** generar solo los tipos y escribir los endpoints a mano (más trabajo y dos lugares que mantener), y escribir todo a mano (se desalinea con el backend).

**5. Carpetas por funcionalidad (feature), con reglas de quién importa a quién.**

```
frontend/src/
  app/            rutas y layouts; solo arman la página
  store/          el almacén de Redux, su proveedor y los hooks tipados
  features/
    catalog/      tarjetas y listados de productos
    cart/         el carrito y su guardado en el navegador
    scheduling/   selector de zona y de franja
    checkout/     lo elegido en el checkout y sus formularios
    payment/      opciones de pago, resultado y mensajes
    admin/        el panel (después de la fase 4)
  shared/
    api/          lo generado por RTK Query, el fetch del servidor y los errores
    ui/           botones, campos, diálogos
    lib/          formato de dinero y de fechas
```

- **La dirección es una sola:** `app → store → features → shared`. Nunca al revés.
- `store` va encima de `features` porque junta las piezas de todas.
- Cada funcionalidad tiene un `index.ts` con lo que ofrece a las demás. Nadie entra a los archivos internos de otra.
- Una herramienta (`eslint-plugin-boundaries`) hace fallar la revisión si alguien rompe la regla. Es el equivalente de ArchUnit en el backend (ADR 0001).
- Las funcionalidades reflejan los módulos del backend (catálogo, agenda, pedidos, pagos).
- **Convenciones:**
  - El código va en inglés, como el backend. Los textos y las rutas van en español: `/catalogo`, `/producto/[slug]`, `/carrito`, `/pago/resultado`.
  - Cada prueba va al lado de su archivo. Las pruebas de punta a punta (E2E) van en una carpeta aparte.
- **Alternativa descartada:** carpetas por capa (`components/`, `hooks/`, `slices/`). Es lo habitual con Vite, pero para tocar una funcionalidad hay que abrir cuatro carpetas y todo termina mezclado.

**6. Interfaz: Tailwind con shadcn/ui.**
- shadcn/ui no se instala como librería: copia el código de cada componente a `shared/ui/`. El código es propio y se ajusta a la marca.
- **Ningún color va fijo en un componente.** Todos salen de variables (fondo, texto, acento, bordes). La paleta todavía no está decidida, y cambiarla debe ser editar un solo archivo.
- El fondo blanco es el punto de partida.
- Formularios con React Hook Form y Zod, con los mismos límites que valida el backend.
- Cada pantalla contempla sus cuatro estados: cargando, vacío, error y con datos.
- **Por qué:** la tienda necesita identidad propia; los componentes son accesibles (teclado y lectores de pantalla); y Tailwind no agrega JavaScript a las páginas de servidor.
- **Alternativas descartadas:** Mantine (completa, pero su aspecto cuesta más de adaptar a una marca), MUI (pesada y con configuración extra para las páginas de servidor) y todo propio (tarda mucho, y la accesibilidad es difícil de hacer bien).

**7. Guardar datos: el navegador llama directo a la API.**
- Crear el pedido, iniciar el pago y las acciones del panel se hacen con los hooks de RTK Query contra `/api/v1`.
- No se usan Server Actions ni un intermediario en Next.js.
- **Por qué:**
  - Es un solo camino para leer y para guardar.
  - Respeta el ADR 0015: Next.js solo muestra; las reglas están en Spring Boot.
  - El carrito y el checkout no pasan por el servidor de Next.js, así que no gastan su memoria.
  - Nginx ya pone la tienda y la API en el mismo dominio, así que no hay problemas de CORS.
- Que la API se vea desde el navegador es normal: la seguridad está en el backend.

**8. Primero el celular, con una meta medible.**
- Se diseña primero para celular y después se amplía a tableta y computador.
- Botones y campos cómodos para el dedo, sobre todo en la franja y en el checkout.
- **Meta:** el catálogo muestra su contenido principal en menos de 2,5 segundos en el perfil móvil de Lighthouse, que simula un celular modesto con red lenta.
- Se mide antes de optimizar, y se optimiza solo donde la medición lo pida. El plan ya exige registrar Lighthouse (fase 5).
- Lo que más pesa en una tienda de flores son las fotos, no las librerías (ver Pendiente).

## Detalles orientativos (recomendaciones sin aprobar; se confirman al implementar)
- **Guardar el carrito:** un middleware propio y corto con `createListenerMiddleware`, que ya trae Redux Toolkit, en lugar de `redux-persist`, que lleva años con poco mantenimiento.
- **Carga inicial del carrito:** el servidor siempre pinta el carrito vacío; el navegador lo llena después de cargar la página. Mientras tanto, el contador muestra un estado neutro. Así el HTML del servidor y el del navegador coinciden.
- **Si la medición lo pide,** RTK Query se carga solo en el checkout y en el panel, no en las páginas públicas.
- **Una imagen para todos los ambientes (ADR 0007):** las variables `NEXT_PUBLIC_*` quedan fijas al construir la imagen. El navegador usa rutas relativas (`/api/v1`), y el servidor de Next.js lee la dirección interna del backend al arrancar.
- **Fechas de las franjas:** llegan como fecha y hora sin zona horaria. Se muestran como texto, sin convertirlas con `new Date`, porque en Bogotá aparecerían un día antes.
- **Dinero:** confirmar cómo envía el backend los montos y mostrarlos con el formato de pesos colombianos.
- **Versión de Next.js:** su caché cambió entre versiones mayores (ADR 0015). Antes de escribir código se revisa la versión vigente en la documentación oficial.
- **Patrones:** hooks propios como "modelo de la pantalla"; un registro por pasarela y medio para su nombre e ícono; tipos que obligan a cubrir cada estado del pago; y las reglas del carrito como funciones puras, sin React.
- **Orden de construcción propuesto:** (1) base del proyecto con Docker y Nginx; (2) catálogo y producto; (3) carrito; (4) checkout; (5) pago y resultado; (6) pulido, accesibilidad, Lighthouse y E2E. El panel, después de la fase 4.

## Pendiente
- **Propuesta de diseño y paleta de colores.** Se hará a partir de tres referencias, adaptadas al proyecto: limpia, minimalista y elegante. No incluye lo que la tienda no tiene (filtros por color o talla, lista de deseos, blog, boletín, registro de clientes, descuentos). Las reseñas llegan en la fase 6.
- **Cómo se sirven las fotos** (fase 4 del plan). Optimizarlas al vuelo en Next.js gasta procesador del servidor; se decide pensando en eso.
- **Lo que le falta al backend para la página de resultado:** consultar el estado del pago y el pedido (pendiente 34 de `docs/pendientes.md`). Una demostración completa del flujo de compra lo necesita.
- **Inicio de sesión del panel** (fase 4). Con la tienda y la API en el mismo dominio, una cookie segura funciona sin cambios en esta arquitectura.

## Consecuencias
- **A favor:**
  - Se aprovecha lo que ya se sabe de Redux Toolkit.
  - El frontend y el backend no se desalinean sin que el CI avise.
  - Las páginas que importan para el SEO llegan armadas desde el servidor, y el servidor trabaja poco.
  - Cambiar la paleta es editar un archivo.
  - Las reglas de carpetas se hacen cumplir solas, como en el backend.
- **En contra:**
  - Redux Toolkit con RTK Query pesa más que las alternativas livianas. Se vigila con la meta del punto 8.
  - El código generado no se edita; hay que acostumbrarse a regenerarlo.
  - Las reglas de carpetas piden disciplina al principio.
  - Hay que aprender las clases de Tailwind.

## Relación con otros ADR
| ADR | Relación |
|---|---|
| 0001 | Misma idea en el frontend: módulos con reglas de dependencia verificadas por una herramienta |
| 0007 | La prueba de contrato y "una imagen para todos los ambientes" condicionan los puntos 4 y los detalles orientativos |
| 0014 | El carrito vive en el navegador (punto 2) |
| 0015 | Este ADR lo complementa: el 0015 dice qué se renderiza dónde; este, cómo se construye |
| 0016 | Los mensajes al cliente (A.23) y la página de resultado dependen del estado que informe el backend |
