# Plan: tienda de arreglos florales (proyecto de portafolio)

**Objetivo:** proyecto corto y bien hecho, que una florista de Bogotá pueda usar si quiere. Si no lo usa, queda como pieza de portafolio. Se construye con rigor desde el inicio, porque puede ser real.

**Stack:** Spring Boot, Angular (SSR), PostgreSQL, Docker, Nginx, AWS, GitHub Actions, Terraform.

**Estado del plan:** todo se desarrolla en local (Docker Compose y Floci) y el despliegue real en AWS se hace **al final, en la cuenta de ella**.

---

## Cómo leer este documento

- **Las secciones numeradas (1 a 17) son temas.** Responden a "qué decidimos y por qué". Son material de consulta.
- **Las fases (sección 10, de la 0 a la 9) son tiempo.** Responden a "qué hago primero y qué después". Cada fase toma piezas de varias secciones.
- **El Anexo A es el paso a paso de las fases 0 y 1.** Las demás fases se bajan a tareas cuando lleguemos a ellas.
- **El Anexo B es el plan de negocio y marketing**, aparte del proyecto técnico.
- **El Anexo C es un glosario** de los términos técnicos.

Este documento es un plan **general**. No es un paso a paso completo.

---

## 1. Registro de decisiones

| # | Decisión | Elección | Alternativas descartadas |
|---|---|---|---|
| 1 | Repositorio | Monorepo **público**, sin datos reales (A+) | Repo privado (en el plan gratuito de GitHub la protección de `main` no se aplicó); repos separados; carpeta `apps/`; carpeta `deploy/` |
| 2 | Arquitectura | Monolito modular, con arquitectura hexagonal estricta en cada módulo | Microservicios; capas clásicas |
| 3 | Organización del backend | Por módulo (`domain`, `application`, `infra`) | Por capas técnicas |
| 4 | Configuración | Perfiles de Spring y variables de entorno | Un archivo por entorno con datos reales; servidor de configuración |
| 5 | Secretos | Sin claves en GitHub (OIDC) y secretos de la app en SSM Parameter Store, en su cuenta | `.env` en la EC2 |
| 6 | Infraestructura como código | Terraform (compatible con OpenTofu) | CDK; consola a mano |
| 7 | Hosting | EC2 con Docker Compose y RDS | ECS con Fargate; un VPS |
| 8 | Despliegue | GitHub Actions con OIDC, con frontend y backend independientes | SSH; manual |
| 9 | Pagos | Mercado Pago detrás de `PaymentGateway`, con los medios de pago (tarjeta, PSE, Nequi, efectivo en punto de pago, transferencia manual y, opcionalmente, contra entrega) modelados aparte del proveedor | Wompi como segundo adaptador, recomendado si se quiere Nequi nativo |
| 10 | Frontend | Angular, con SSR o prerender por ruta | Next.js; Vercel |
| 11 | Backend | Spring Boot (Java) | C# y .NET |
| 12 | Código y ella | Tú lo mantienes y ella tiene una licencia de uso | Transferirle el repo; una copia; otro desarrollador |
| 13 | Portafolio | El mismo repo, público desde el inicio | Repo espejo; repo privado con capturas |

**Principios que sostienen todo:**
1. **Todo local primero, despliegue al final.** No dependemos de una cuenta de AWS hasta la fase 8. Para amortiguar los sustos del final, el Dockerfile y el compose funcionan "como producción" desde la fase 1, y Terraform se valida contra Floci en la fase 7.
2. **Rebanadas verticales.** Cada fase entrega algo que funciona de punta a punta, no "todo el backend" y luego "todo el frontend".
3. **Alcance en tres niveles** (sección 11). Si el tiempo aprieta, se recorta lo opcional.
4. **Fronteras verificadas.** Pruebas de arquitectura que fallan si alguien rompe las reglas (sección 3).
5. **Abstracciones solo donde hay un tercero que cambia o estorba en las pruebas.**
6. **Nada real dentro del repositorio** (sección 15).

---

## 2. Arquitectura y estructura del repositorio

```
Navegador → Nginx (HTTPS, caché) ─┬─ /       → Angular SSR (Node)
                                   └─ /api/v1 → Spring Boot
                                                 ├─ PostgreSQL (RDS)
                                                 ├─ S3 (fotos)
                                                 ├─ SQS → SES (correos)
                                                 ├─ SSM Parameter Store
                                                 └─ Mercado Pago (adaptador)
```

**Estructura del repositorio:**
```
tienda-florales/
├── backend/                  # Spring Boot + su Dockerfile
├── frontend/                 # Angular + su Dockerfile
├── proxy/                    # configuración de Nginx (se crea en la fase 1)
├── infra/                    # Terraform genérico, sin valores reales
├── docs/
│   ├── plan.md
│   └── decisions/            # decisiones de arquitectura (ADR)
├── .github/workflows/
├── docker-compose.yml        # local, con Floci
├── docker-compose.prod.yml   # producción, sin Floci (se crea en la fase 7)
├── .env.example              # variables de ejemplo, sin valores reales
└── README.md
```

**Monolito modular hexagonal.** Módulos: catálogo, agenda (cupos), pedidos, pagos, reseñas, notificaciones e identidad, más un `shared` mínimo. Cada módulo se organiza así:

```
backend/src/main/java/.../<módulo>/
├── domain/        reglas de negocio, entidades puras y puertos (interfaces)
├── application/   casos de uso y la API pública del módulo
└── infra/         adaptadores: controladores REST, JPA, S3, SES, Mercado Pago
```

**Sin API Gateway ni microservicios.** Nginx cumple el papel de punto de entrada.

### Seguridad
- Spring Security con `user`, `role` y `user_role` (muchos a muchos).
- Roles: `ADMIN` y `EDITOR`.
- Los clientes compran como invitados, sin cuenta.

### Nginx
Sirve para tres cosas: HTTPS (exigido por la pasarela y favorable al SEO), servir estáticos con compresión y caché, y ser el único punto de entrada (`/` va a Angular y `/api/v1` a Spring Boot, que no se expone a internet). Alternativa más simple: Caddy, que renueva el HTTPS solo.

### Angular y SSR
- Prerender o SSR para las páginas públicas (inicio, catálogo, producto) y cliente para carrito, checkout y panel admin.
- Estructurar el proyecto con `@angular/ssr` desde el inicio permite activar SSR por ruta después, sin reescribir.
- Estado con signals y servicios, sin NgRx.
- **Medir la memoria del servidor de Node en local** antes de elegir el tamaño de la EC2.
- Vercel descartado: el plan gratuito (Hobby) es solo para uso no comercial, y separar frontend y API complica el despliegue.

---

## 3. Arquitectura hexagonal: reglas y abstracciones

**Reglas de dependencia** (se hacen cumplir con ArchUnit o Spring Modulith, no con buena voluntad):
1. `domain` no depende de Spring, JPA, el SDK de AWS ni de `application` o `infra`.
2. `application` depende solo de `domain`.
3. `infra` puede depender de `domain` y `application`, nunca al revés.
4. Un módulo no usa las clases internas de otro: solo su API pública en `application`.
5. Cada módulo es dueño de sus tablas. Otro módulo no las consulta directo.
6. No hay ciclos entre módulos.

**Entidades:** las del dominio son puras y las de JPA viven en `infra`, con mapeadores entre ambas. Es más código, pero es el precio del rigor y lo que hace comprobable la regla 1.

**Los puertos:** `PaymentGateway`, `FileStorage`, `EmailSender` y `Clock`, además de los repositorios de cada módulo. Los servicios sin segunda implementación posible y que no tocan el exterior no llevan interfaz. La cola SQS se usa directa al inicio.

**Pagos: `PaymentGateway`**

```java
public interface PaymentGateway {
    String provider();                                   // "mercadopago"
    CheckoutSession createCheckout(PaymentRequest req);  // devuelve URL de pago
    Optional<PaymentEvent> parseWebhook(RawWebhook raw); // valida firma y normaliza
    PaymentStatus fetchStatus(String providerPaymentId); // para conciliar
}
```

- **Patrones:** puerto y adaptador, Strategy, registro por proveedor (`Map<String, PaymentGateway>` inyectado por Spring) y capa anticorrupción (el vocabulario de Mercado Pago se traduce a tu `PaymentStatus` en el borde).
- **Proveedor principal:** Mercado Pago (Checkout Pro). Wompi queda como segundo adaptador: opcional, pero recomendado si se quiere Nequi nativo (ver más abajo). Tener dos además demuestra que la abstracción funciona.
- **Guardar `provider` en cada pago**, para que los pagos viejos sigan resolviéndose si se cambia de proveedor.
- **Idempotencia:** tabla de eventos procesados con restricción única `(provider, event_id)`.
- **Seguridad del webhook:** validar la firma (revisar en la documentación de Mercado Pago el mecanismo exacto) y reconfirmar el estado consultando la API del proveedor. Nunca confiar en el cuerpo recibido.
- **El pedido pasa a pagado solo por webhook**, nunca porque el navegador del cliente lo diga.
- **Comisiones y tiempos de acreditación en Colombia:** no se verificaron. Compararlos con Wompi antes de elegir para producción.

**Medios de pago: se separa "cómo paga el cliente" de "quién procesa el pago"**

- **Medio de pago** (`PaymentMethod`): tarjeta, PSE, Nequi, efectivo en un punto de pago (Efecty), transferencia manual y contra entrega.
- **Proveedor** (`provider`): Mercado Pago, Wompi o `manual`.
- Cada medio tiene una **política** configurable (`PaymentMethodPolicy`): si está habilitado, cómo se confirma, cuánto tiempo se retiene el cupo y qué límites tiene (monto máximo, zonas, anticipación mínima).

| Tipo | Medios | Cómo se confirma | Retención del cupo |
|---|---|---|---|
| En línea | Tarjeta, PSE, Nequi | Webhook del proveedor | 10 a 15 minutos |
| En línea diferido | Efectivo en un punto de pago (Efecty) | Webhook cuando el cliente paga en el punto, antes de que venza el comprobante | Hasta el vencimiento del comprobante, sin pasar del límite de anticipación de la entrega |
| Manual | Transferencia a Nequi o a la cuenta de ella | El admin la confirma tras verificarla en la app del banco | Configurable (unas horas, por ejemplo), con aviso al cliente |
| Contra entrega | Efectivo al recibir | Se registra el cobro al entregar | El cupo se reserva al crear el pedido |

Reglas:
- **`PaymentGateway` no cambia:** sigue cubriendo a los proveedores en línea. La transferencia manual y el contra entrega son casos de uso propios (`ConfirmarPagoManual` y `RegistrarCobroContraEntrega`), con el mismo ciclo de estados del pago (`PENDING`, `PAID`, `FAILED`, `EXPIRED`, `CANCELLED`) y los mismos controles (idempotencia y registro de quién hizo qué y cuándo).
- **Solo el admin confirma pagos manuales**, y se verifica en la app del banco o de Nequi, nunca por una captura de pantalla, porque los comprobantes falsos existen.
- **Contra entrega viene deshabilitado por defecto.** El producto es perecedero y hecho por encargo: si el cliente lo rechaza, se pierde. Si se habilita (decisión con ella), va con límites (monto, zonas) y confirmación previa.
- **Qué ofrece cada proveedor** (revisado en documentación de terceros y del proveedor; confirmar en el sandbox): Wompi lista tarjetas, PSE, Nequi, Daviplata y opciones de Bancolombia; Mercado Pago en Colombia ofrece en Checkout Pro tarjetas, saldo de la cuenta y PSE o Efecty, y Nequi pasaría por PSE.

---

## 4. Problema estrella: control de cupos

El corazón técnico del proyecto y el mejor tema de entrevista.

- **Reserva atómica en la base de datos:** `UPDATE delivery_slot SET booked = booked + 1 WHERE id = ? AND booked < capacity`. Si no afecta ninguna fila, el cupo está lleno.
- **El puerto lo expresa en términos de negocio** (por ejemplo, `reservarCupo(franja)` que devuelve éxito o fallo) y el adaptador JPA ejecuta el SQL. La regla vive en el dominio, y la base de datos la hace cumplir.
- **Restricción en la base de datos:** `CHECK (booked <= capacity)` como segunda defensa.
- **Reserva temporal** mientras el cliente paga, con duración según el medio de pago (10 a 15 minutos para los pagos en línea; más larga para efectivo en un punto o transferencia manual, según su política). Un job libera las reservas vencidas.
- **Prueba de concurrencia:** 50 peticiones simultáneas para 10 cupos; deben tener éxito exactamente 10. Con Testcontainers y PostgreSQL real.

---

## 5. Modelo de datos

- `product` (nombre, descripción, precio, activo) y `product_image` (URL en S3, orden)
- `customer` (nombre, teléfono, correo)
- `delivery_zone` (nombre, costo de envío)
- `delivery_slot` (fecha, franja, `capacity`, `booked`)
- `orders` (cliente, zona, franja, dirección, estado, total)
- `order_item` (producto, cantidad, **precio al momento de la compra**)
- `payment` (orden, `provider`, `method`, referencia única, estado y, en los manuales, quién confirmó y cuándo)
- `payment_event` (provider, event_id, restricción única)
- `review` (orden, producto, calificación, texto, estado de moderación, respuesta del dueño)
- `user`, `role`, `user_role`

**Las tablas se crean en la fase que las necesita**, no todas de golpe. La fase 1 solo crea `product` y `delivery_slot`.

**Reglas de datos:**
- Dinero con `BigDecimal` o enteros, nunca con `double`.
- Fechas y horas en UTC (`timestamptz`) y se muestran en hora de Bogotá.
- Migraciones con Flyway, solo hacia adelante: una migración ya aplicada no se edita. Las de esquema corren en todos los entornos; las de datos de ejemplo, solo en `local`.
- Cambios compatibles hacia atrás (agregar, migrar y recién después retirar).
- Índices en lo que se consulta y paginación en los listados.

**Reseñas:** solo de quien tenga un pedido entregado, una por pedido, moderadas por el admin. Datos estructurados (`Review`, `AggregateRating`) solo con reseñas reales.

---

## 6. Pruebas

| Tipo | Herramienta | Qué cubre |
|---|---|---|
| Unitarias | JUnit 5 + Mockito | Servicios con dependencias simuladas (pago, correo, S3, reloj). Casos: cupo lleno, pedido ya pagado, reseña sin pedido entregado |
| Slice | `@WebMvcTest` + Mockito | Controladores, validaciones y errores globales, con el caso de uso simulado |
| Integración | Testcontainers (PostgreSQL y Floci) | Repositorios, migraciones y concurrencia de cupos |
| Contrato | Suite compartida | La misma batería contra `FakePaymentGateway` y contra el adaptador de Mercado Pago (API simulada con WireMock) |
| Webhooks | Integración | Firma inválida, evento duplicado y estados desordenados |
| Medios de pago | JUnit 5 + Mockito | Máquina de estados del pago, política de cada medio (retención del cupo, límites), confirmación manual solo para el admin y sin duplicarse, contra entrega deshabilitado por defecto |
| Arquitectura | ArchUnit / Spring Modulith | Las reglas de dependencia de la sección 3 |
| Cobertura | JaCoCo | Reporte en CI, sin obsesionarse con el porcentaje |
| Seguridad | Spring Security Test | Endpoints de admin protegidos, roles `ADMIN` y `EDITOR`, acceso anónimo denegado donde corresponde |
| Frontend | Runner de pruebas del proyecto Angular | Componentes y servicios (carrito, selección de franja) |
| Extremo a extremo (E2E) | Playwright | Flujo completo: elegir arreglo, elegir franja, pagar en sandbox |
| Humo (smoke) | Script o prueba simple | Tras desplegar: salud de la API, una página pública y una reserva |

**Orden de aprendizaje:** primero JUnit y Mockito sobre un servicio sencillo (pedidos), luego Testcontainers con los cupos.

**Qué hace cada herramienta (y dónde no se usa):**
- **JUnit 5** es el motor de todas las pruebas del backend (unitarias, slice, integración, concurrencia y arquitectura).
- **Mockito** simula dependencias y se usa solo en las **unitarias** y en las **slice**. Ejemplo: probar que un pedido ya pagado no se cobra de nuevo, con el `PaymentGateway` simulado.
- **Fakes escritos a mano** (`FakePaymentGateway`, `FakeEmailSender`, un `Clock` fijo) para los puertos que tienen suite de contrato o que se reutilizan en muchas pruebas.
- **Las pruebas de integración y de concurrencia no usan Mockito:** corren contra PostgreSQL real con Testcontainers. Solo se simula el borde externo que no se puede levantar (Mercado Pago, con WireMock).
- **El frontend no usa JUnit ni Mockito:** tiene su propio runner (el del proyecto Angular) y Playwright para el E2E.

**Regla de calidad:** ninguna funcionalidad se da por terminada sin sus pruebas, y el CI bloquea el merge si fallan. Cada fase tiene su línea de **Pruebas** en la sección 10, y cada una cumple su criterio antes de pasar a la siguiente.

---

## 7. Entornos, Docker y migración a AWS

**Lo que migra a AWS es la imagen de Docker de tu app. Floci es solo un andamio de desarrollo.**

| | Local | AWS real |
|---|---|---|
| App y Nginx | contenedores en `docker-compose.yml` | los mismos contenedores, en `docker-compose.prod.yml` |
| PostgreSQL | contenedor | RDS (cambia la URL) |
| S3, SQS, SES, secretos | Floci en `localhost:4566` | servicios reales |
| Credenciales | falsas | rol IAM de la EC2 |

**Reglas para que migrar sea fácil:**
1. Una imagen por componente y por commit (`backend` y `frontend`); cambia la configuración, no la imagen.
2. Perfiles de Spring `local`, `test` y `prod`. La configuración `local` se commitea porque solo tiene valores de ejemplo, y las variables reales entran por entorno.
3. El endpoint de Floci solo existe en `local` (y quizá acceso S3 por ruta, solo ahí).
4. Credenciales por la cadena por defecto del SDK. Nada de llaves en el repositorio.
5. Prueba de humo final contra AWS real, porque Floci no garantiza paridad total.

**Hosting inicial:** una EC2 pequeña con Docker Compose (backend, frontend y Nginx) y RDS para la base de datos. ECS y Fargate quedan para más adelante.

**Pagos y `localhost`:** los webhooks reales no llegan a `localhost`. En desarrollo, un túnel temporal (ngrok o Cloudflare Tunnel); en CI, WireMock.

**Entornos:** local (compose y Floci), CI (efímero) y un solo entorno real en AWS, en la cuenta de ella. Un segundo AWS duplica el costo.

**Configuración por entorno:** perfiles de Spring más variables de entorno. Ningún archivo con valores reales entra al repo.

---

## 8. CI/CD (GitHub Actions)

**Pull request, un único workflow `ci`:**
- Un job de **detección de cambios** decide si corren las pruebas del backend, las del frontend o ambas.
- Los jobs de pruebas compilan, prueban (unitarias, integración, arquitectura) y reportan cobertura.
- Desde la fase 5, cuando existe el frontend, el **E2E corre contra todo el sistema** en cada PR, aunque solo cambie una parte.
- Un job final `ci-ok` (un check que resume a todos los demás y corre siempre) es el único check obligatorio en la protección de la rama (ver el glosario del Anexo C). Así los workflows filtrados por rutas no dejan un check "pendiente" que bloquee el merge.

**Merge a `main`, despliegues independientes:**
- Un workflow de despliegue del **backend** (se dispara con cambios en `backend/**`) y otro del **frontend** (`frontend/**`). Cada uno construye su imagen, la sube a ECR y reinicia solo su contenedor (`docker compose up -d <servicio>`).
- Autenticación con **OIDC**, sin llaves permanentes, y aprobación manual en producción.
- Se **escriben en la fase 7 y se activan en la fase 8**.

**Compatibilidad entre frontend y backend** (porque se despliegan por separado):
- API versionada (`/api/v1`) y descrita con OpenAPI.
- Cambios compatibles hacia atrás: primero el backend que acepta lo viejo y lo nuevo, luego el frontend, y al final se retira lo viejo.
- Las migraciones corren con el arranque del backend (Flyway), así que solo afectan a su propio despliegue.
- Cada despliegue implica un reinicio breve del servicio que cambió; cero downtime exigiría balanceador y dos instancias, y hoy no hace falta.

**Floci en el CI**, dos formas:

A. Como servicio del workflow:
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      floci:
        image: floci/floci:<versión>   # fijar la versión, no usar latest
        ports: ["4566:4566"]
    env:
      AWS_ENDPOINT_URL: http://localhost:4566
      AWS_ACCESS_KEY_ID: test
      AWS_SECRET_ACCESS_KEY: test
      AWS_REGION: us-east-1
    steps:
      - uses: actions/checkout@v4
      - run: ./mvnw verify
```

B. Con Testcontainers (módulo `testcontainers-floci`): la opción recomendada, porque corre igual en tu PC y en el CI, y el contenedor se destruye al terminar.

**Infraestructura como código:** Terraform (EC2, RDS, S3, SQS, SES, ECR, IAM), validado primero contra Floci (fase 7) y aplicado en AWS real (fase 8). Verificar el nombre y la versión de la imagen de Floci en su README antes de fijarla. Terraform es de uso general: el mismo lenguaje sirve si algún día se cambia de proveedor.

---

## 9. AWS: costos y avisos

- **La cuenta es la de ella.** Pedirle que cree un usuario IAM para ti con permisos limitados y MFA. No compartir contraseñas ni credenciales del root.
- **La factura le llega a ella.** Configurar alertas de presupuesto desde el primer día.
- Las cuentas nuevas ya no tienen "12 meses gratis": reciben créditos (US$100, hasta US$200). **El plan gratuito se cierra a los 6 meses o al agotar los créditos.** Elegir el plan de pago.
- Si ella ya tenía cuenta, es posible que no le apliquen los créditos de cuentas nuevas. Que lo revise.
- **Los plazos de AWS empiezan al abrir la cuenta.** Por eso se abre al final, no antes.
- Evitar NAT Gateway y balanceadores de carga al inicio.
- **Secretos:** SSM Parameter Store, en su nivel estándar (los parámetros no tienen costo de almacenamiento). Secrets Manager cobra por cada secreto al mes y solo se justificaría si se necesitara rotación automática.
- **Estado de Terraform:** guardarlo en un bucket de S3 remoto, con versionado y cifrado, porque puede contener datos sensibles. Ese bucket se crea primero, antes del resto.
- SES arranca en modo de pruebas (solo envía a direcciones verificadas): solicitar la salida a producción con tiempo.
- App Runner está cerrado a nuevos clientes: no contar con él.
- Para desarrollar sin gastar: Docker Compose local con PostgreSQL y Floci.

---

## 10. Fases

**Fase 0: Preparación (1 día).** Repositorio, estructura, archivos base, decisiones por escrito y reglas de trabajo.

**Fase 1: Esqueleto local (semanas 1 y 2).** Proyecto Spring Boot con la estructura de módulos y ArchUnit desde las primeras pruebas, compose (PostgreSQL, Floci, Nginx), Flyway con las primeras tablas, endpoint de salud, primeras pruebas, CI sin despliegue y Dockerfile. Sin AWS.

*Pruebas:* una unitaria trivial con JUnit 5, una de integración con Testcontainers que verifica las migraciones, una de arquitectura con ArchUnit y el CI corriendo las tres.

**Fase 2: Cupos y catálogo.** Productos, zonas, franjas y reserva atómica de cupos. Los datos de ejemplo entran por migraciones de semilla **que solo carga el perfil `local`** (una carpeta de migraciones aparte que producción no lee), porque la administración llega en la fase 4 y los datos inventados nunca deben llegar a la base real de ella. La API nace versionada (`/api/v1`) y descrita con OpenAPI desde el primer endpoint. Aquí se aprende JUnit, Mockito y Testcontainers.

*Pruebas:* unitarias con JUnit 5 y Mockito de los servicios (cupo lleno, cupo vencido, con el repositorio y el reloj simulados), `@WebMvcTest` de los controladores con el caso de uso simulado con Mockito, y la prueba de concurrencia con JUnit 5 y Testcontainers, **sin simulaciones** (50 peticiones para 10 cupos).

**Fase 3: Pedidos y pagos.** Pedido como invitado, reserva temporal del cupo con vencimiento y job que libera las reservas sin pago (se une al pedido, por eso va aquí), `PaymentGateway` con Mercado Pago en sandbox, webhook, idempotencia y pruebas de contrato, y los medios de pago como concepto propio con su política (tarjeta, PSE, Nequi, efectivo en un punto de pago, transferencia manual y contra entrega, deshabilitado por defecto).

*Pruebas:* contrato compartido con JUnit 5 (`FakePaymentGateway` escrito a mano y adaptador con WireMock), webhook con firma inválida, evento duplicado y estados desordenados (integración con Testcontainers), expiración y liberación de reservas con un `Clock` fijo, y unitarias del flujo de pedido con JUnit 5 y Mockito (por ejemplo, un pedido ya pagado no se cobra de nuevo), más la máquina de estados del pago y la política de cada medio (retención del cupo, límites y contra entrega deshabilitado por defecto).

**Fase 4: Identidad y operación (solo backend).** Login con roles muchos a muchos, API de administración (productos, franjas, pedidos, moderación, confirmación de pagos manuales y registro de cobros contra entrega) y fotos en S3 (contra Floci). La interfaz del panel llega en la fase 5.

*Pruebas:* seguridad con Spring Security Test sobre JUnit 5 (rutas de admin protegidas, roles, acceso denegado; solo el admin confirma pagos manuales y confirmarlos dos veces no los duplica) y subida de fotos con Testcontainers y Floci, sin simulaciones.

**Fase 5: Frontend.** Angular con SSR en catálogo y producto, cliente en carrito, checkout (con los medios de pago disponibles) y la interfaz del panel admin sobre la API de la fase 4, incluida la confirmación de pagos manuales. Dockerfile del frontend y su ruta en el CI. SEO técnico (metadatos, `sitemap.xml`, datos estructurados) y Lighthouse medido en local con la compilación de producción, antes y después de optimizar.

*Pruebas:* unitarias del carrito y la selección de franja con el runner del proyecto Angular (aquí no se usa JUnit ni Mockito), un E2E con Playwright del flujo de compra en sandbox y los resultados de Lighthouse registrados.

**Fase 6: Extras.** Correos asíncronos (SQS a SES) y reseñas moderadas (backend y su interfaz en Angular, incluida la moderación en el panel admin).

*Pruebas:* reglas de reseña con JUnit 5 y Mockito (solo con pedido entregado, una por pedido, moderación), `FakeEmailSender` en las unitarias y el envío por SQS contra Floci con Testcontainers.

**Fase 7: Listo para desplegar.** Terraform genérico validado contra Floci (cubre bien RDS, S3, SQS, SES y ECR; EC2 e IAM tienen soporte parcial, así que esas partes se confirman con `plan` y en AWS real), `docker-compose.prod.yml`, workflows de despliegue del backend y del frontend escritos pero apagados, README técnico con diagrama y decisiones, y el acuerdo de propiedad del código con ella (sección 14).

*Pruebas:* `terraform validate` y `plan` (y `apply` contra Floci), la imagen de producción levantada con el compose de producción y los E2E pasando contra ese entorno.

**Fase 8: Despliegue real en la cuenta de ella.** Primero, el día uno de la fase: pedir la salida a producción de SES (puede tardar) y la activación de Mercado Pago de producción a nombre de ella. Luego: usuario IAM con MFA y alerta de presupuesto, bucket del estado de Terraform, dominio, HTTPS, `terraform apply`, secretos de la app en SSM Parameter Store, rol de OIDC y activación de los pipelines.

*Pruebas:* smoke tests contra AWS real (salud de la API, página pública, una reserva y un pago en sandbox) y repetirlos tras cada despliegue.

**Fase 9: Operación y cierre.** Respaldos de la base de datos y una prueba de restauración, alertas (presupuesto y caída), un documento corto de qué hacer si algo falla, revisión del repositorio antes de publicarlo (incluido el historial), licencia, publicación del repo y capturas en el README.

*Pruebas:* restaurar un respaldo en un entorno de prueba, disparar una alerta a propósito para comprobar que llega, y el escaneo de secretos sobre todo el historial.

**Estimado:** 6 a 8 semanas a medio tiempo para las fases 0 a 7, más **una semana de colchón** para la fase 8 (ahí aparecen juntos los problemas que Floci no detecta) y unos días para la fase 9. No fijar fechas hasta terminar la fase 2: aprender Angular, AWS y pruebas a la vez retrasa.

---

## 11. Prioridades si el tiempo aprieta

- **Imprescindible:** cupos con prueba de concurrencia, pedidos, pago en sandbox, CI, despliegue en AWS y nada real en el repo.
- **Importante:** roles, panel admin, Angular SSR con SEO, Terraform, la transferencia manual con confirmación del admin y los respaldos (si ella va a usarlo de verdad).
- **Opcional:** reseñas, correos asíncronos, Wompi como segundo adaptador, pago contra entrega y Mercado Pago en producción.

---

## 12. Terminado cuando

- `docker compose up` levanta todo.
- La prueba de concurrencia pasa en el CI, junto con las unitarias, de integración, de contrato, de arquitectura, de seguridad y el E2E.
- Un cambio en el frontend se despliega sin reiniciar el backend, y al revés.
- El despliegue en AWS es automático y con HTTPS.
- Un pago de punta a punta funciona en sandbox.
- Lighthouse está medido y documentado.
- Hay respaldos con una restauración probada.
- El README explica la arquitectura y las decisiones, y el repo no tiene datos reales ni secretos, tampoco en el historial.

**Fuera de alcance:** microservicios, varias ciudades, cuentas de cliente con historial y publicidad pagada.

---

## 13. Riesgos

- Sobrecarga de aprendizaje (Angular, AWS y pruebas a la vez).
- **Dejar AWS para el final concentra los sustos** (IAM, memoria del SSR, HTTPS, SES) y depende de la disponibilidad de ella. Mitigación: compose y Dockerfile "como producción" desde la fase 1, Terraform validado en Floci y semana de colchón. Opcional: un VPS barato por un par de días para probar HTTPS y Nginx.
- Memoria del SSR en una EC2 pequeña.
- SES en modo de pruebas y salida a producción.
- Paridad parcial de Floci (en particular EC2 e IAM): confirmar siempre en AWS real.
- Frontend y backend desalineados un momento al desplegarse por separado (mitigación en la sección 8).
- Una credencial o dato real que se cuele en el repositorio o en su historial (más grave ahora que el repo es público).
- Costos de AWS al agotarse los créditos, pagados por ella.
- Falta de un acuerdo claro sobre el código, las cuentas y el dinero (sección 14).

**Pendientes por verificar** (no los confirmé):
- Comisiones y tiempos de acreditación de Mercado Pago en Colombia, y qué medios (Nequi, Efecty, PSE) ofrece realmente en el sandbox.
- Si se habilita el pago contra entrega y con qué límites (decisión con ella).
- Condiciones de la cuenta de Nequi o del banco de ella para recibir pagos de un negocio (límites y uso comercial).
- Si a su cuenta de AWS le aplican créditos de cuenta nueva.
- Nombre y versión de la imagen de Floci, y el soporte real de EC2 e IAM.
- Si las pasarelas de pago exigen constitución legal para emitir las llaves de producción.
- Cómo se sirven las fotos (bucket público, URLs firmadas o CloudFront). Se decide en la fase 4, mirando SEO, costo y privacidad.

---

## 14. Propiedad del código y acuerdo con ella

**Protección del software en Colombia:** por regla general se protege por **derecho de autor**, no por patente (los programas de computador, en principio, no son patentables). El derecho de autor nace al crear la obra, sin trámite. El registro ante la Dirección Nacional de Derecho de Autor es voluntario y da una prueba más fuerte. Confirmar con un abogado.

**Qué pasa con el código si ella lo usa:**

| Opción | Cómo funciona | Cuándo conviene |
|---|---|---|
| **Tú mantienes, con licencia a ella (elegida)** | El código sigue en tu repo; ella usa la tienda desplegada en su cuenta y el panel admin, sin tocar el repo | Mientras sigas involucrado |
| Transferirle el repo | Se cambia el dueño del repo y los secretos | Si ya no quieres seguir |
| Una copia o fork | Ella tiene su propia copia | Se desactualiza con el tiempo |
| Otro desarrollador | Necesita acceso al repo | Se resuelve con la transferencia |

Como el repo no tiene nada real, cualquier traspaso es limpio: se cambia el dueño y los secretos, sin reescribir nada.

**Medidas prácticas:**
- Repositorio en tu cuenta de GitHub, **público desde el inicio** (en el plan gratuito, la protección de `main` y el escaneo de secretos solo se aplican en repos públicos), con historial de commits como prueba de autoría.
- **Licencia explícita desde el primer día público.** Propuesta inicial: *todos los derechos reservados* (el código se puede ver, pero no reutilizar). Se revisa cuando se cierre el acuerdo con ella, o si prefieres que sea reutilizable (por ejemplo, con MIT o Apache-2.0). Ten presente que, al publicar en GitHub, otros usuarios pueden ver y bifurcar (fork) el repositorio dentro de la plataforma, según sus términos. Confirmar con un abogado.
- **Acuerdo corto con ella**, aunque sea un mensaje aclarado: el código es tuyo y ella tiene licencia para usarlo en su negocio; de quién es el dominio, el Instagram y las cuentas; qué pasa si cada uno sigue por su lado; porcentaje y quién factura.
- Lo que no es tuyo: su marca, sus fotos y su catálogo. Aclararlo, y como el repo es público, nada de eso debe estar dentro.

Hacerlo **antes de la fase 8**, porque el despliegue ocurre en su cuenta.

---

## 15. Rigor y convenciones del repositorio

**Definición de "terminado" para cualquier tarea:** código, pruebas, una decisión escrita si se decidió algo, CI en verde y nada real en el repositorio.

**Nada real en el repo:**
- Sin secretos, sin IDs de cuenta ni de pasarela, sin datos ni fotos de ella. Los datos de ejemplo son inventados y las fotos de prueba son propias o libres.
- Como el repositorio es público, cualquier cosa que se cuele se hace pública de inmediato: se revisa antes de cada commit y se vigila con el escaneo de secretos.
- Los valores reales viven en variables de entorno, en secretos de GitHub y en SSM Parameter Store. Un `.env.example` documenta qué variables existen.
- `infra/` es genérico (módulos y variables); los `.tfvars` reales no se commitean.

**Trabajo:**
- Rama `main` protegida, todo por pull request, con el check `ci-ok` obligatorio.
- Commits con prefijo (`feat:`, `fix:`, `test:`, `chore:`, `docs:`) y PRs pequeños.
- Una decisión de arquitectura nueva se escribe en `docs/decisions/` antes de implementarla.

**Cadena de suministro y seguridad:**
- Versiones de dependencias e imágenes fijadas (nunca `latest`).
- Dependabot, escaneo de secretos (el de GitHub, gratuito en repos públicos, y gitleaks en el CI, también sobre el historial) y escaneo de imágenes y dependencias en el CI.
- Contenedores con usuario no root.

**Datos personales (Ley 1581):** pedir solo los datos necesarios, no escribirlos en los logs y tener la política de tratamiento publicada.

**Observabilidad básica:** logs estructurados sin datos personales, health checks y un identificador de correlación por petición.

**API:** versionada (`/api/v1`), descrita con OpenAPI y con cambios compatibles hacia atrás.

---

## 16. Operación y continuidad (si es real)

Esto aparece solo si ella usa la tienda de verdad, y se hace en la fase 9:
- **Respaldos:** automáticos de la base de datos con retención definida y **una restauración probada**. Un respaldo que nunca se restauró no cuenta.
- **Alertas:** de presupuesto y de caída (un chequeo externo al health check).
- **Logs** con retención limitada, para controlar costos.
- **Actualizaciones:** parches del sistema de la EC2, imágenes base y dependencias.
- **Rotación de secretos** y revocación de accesos cuando alguien deja de participar.
- **Documento corto de qué hacer si algo falla:** quién avisa a quién, cómo ver los logs, cómo volver a la versión anterior y cómo restaurar.

Verificar los costos de cada pieza antes de activarla.

---

## 17. Portabilidad: si se cambia de AWS a un VPS

El costo de migrar es de días, no de semanas, porque el código no está acoplado a AWS.

| Pieza | En AWS | En un VPS | Qué toca cambiar |
|---|---|---|---|
| App, Nginx, frontend | contenedores | los mismos contenedores | nada |
| Base de datos | RDS | PostgreSQL en contenedor | URL de conexión; **los respaldos pasan a ser tuyos** |
| Fotos | S3 | disco, MinIO o seguir con S3 | un adaptador de `FileStorage` |
| Correos | SES | otro servicio o SMTP | un adaptador de `EmailSender` |
| Cola de correos | SQS | cola en base de datos o en memoria | un adaptador, o simplificar |
| Secretos | SSM Parameter Store | `.env` o Docker secrets | configuración |
| Infraestructura | Terraform para AWS | otro proveedor de Terraform o scripts | reescribir `infra/` |
| Despliegue | GitHub Actions con OIDC | GitHub Actions por SSH | los workflows |

**Lo que no cambia:** el dominio, los casos de uso, los pagos, las pruebas y el frontend. **Lo que se pierde:** lo gestionado (respaldos, parches y recuperación ya resueltos en RDS) y el valor de portafolio de haber desplegado en AWS.

---

## Anexo A: paso a paso, fases 0 y 1

Cada paso tiene un criterio de "terminado". No pasar al siguiente hasta cumplirlo.

### Fase 0: preparación

**0.1 Repositorio.** Crear el repo vacío en GitHub y clonarlo. Nació privado y pasa a público en el paso 0.6.
*Terminado cuando:* la carpeta `tienda-florales` existe en tu computador.

**0.2 Estructura de carpetas.** `backend/`, `frontend/`, `infra/`, `docs/decisions/`, `.github/workflows/`, cada una con un `.gitkeep`, y un `docker-compose.yml` vacío en la raíz. La carpeta `proxy/` se crea en el paso 1.6.
*Terminado cuando:* las carpetas existen.

**0.3 Archivos base** en la raíz:
- `.gitignore`, **sin** la línea `application-local.yml` (la configuración local con valores de ejemplo sí se commitea; así cualquiera clona y levanta todo). Debe ignorar `.env`, claves (`*.pem`), el estado y variables de Terraform, y las carpetas de IDEs, Java y Node.
- `.env.example` con los nombres de las variables y valores de ejemplo (se ignora `.env`, pero **no** `.env.example`).
- `README.md` mínimo.
- El plan copiado a `docs/plan.md`, que desde ese momento es la **fuente de verdad**: se edita en el repo y cualquier otra copia es solo una foto.
*Terminado cuando:* los cuatro archivos existen.

**0.4 Decisiones por escrito** en `docs/decisions/`, de media página cada una, con contexto, decisión y consecuencias:
1. Monolito modular con arquitectura hexagonal estricta.
2. Monorepo público sin datos reales.
3. Medios de pago y `PaymentGateway` como puerto y adaptador.
4. Floci como andamio de desarrollo, no como dependencia.
5. Terraform como infraestructura como código.
6. Secretos y configuración (perfiles, variables de entorno, OIDC).
7. Despliegue independiente de frontend y backend.
8. Propiedad del código y licencia.
*Terminado cuando:* están los 8 archivos.

**0.5 Primer commit y push** a `main`, mientras todavía no está protegida.
*Terminado cuando:* el repo en GitHub muestra las carpetas y los archivos base.

**0.6 Reglas de trabajo.** Ahora sí, proteger `main` (todo por PR), usar commits con prefijo y activar Dependabot y el escaneo de secretos. En el plan gratuito de GitHub la protección de `main` y el escaneo de secretos solo se aplican en repos públicos (se comprobó: en privado, un push directo a `main` no fue rechazado), así que el repo se hace público en este paso, con su licencia, y después se repite la prueba de protección. Desde aquí, todo cambio va en una rama y entra por pull request.
*Terminado cuando:* no se puede empujar directo a `main`. El check obligatorio `ci-ok` se agrega en el paso 1.5, cuando exista.

### Fase 1: esqueleto local

**1.1 Proyecto Spring Boot** en `backend/`, creado en start.spring.io con Maven, la versión estable que ofrezca por defecto (nunca SNAPSHOT, milestone ni RC), la última LTS de Java soportada y **solo** Web, Validation y Actuator. Las demás dependencias se agregan en el paso que las necesita (JPA, PostgreSQL y Flyway en el 1.3; Testcontainers y ArchUnit en el 1.4), porque con JPA y un datasource la app no arranca sin base de datos, y esa no existe hasta el 1.2. Estructura de paquetes por módulo con `domain`, `application` e `infra` (se materializa con archivos `package-info.java`; las reglas de ArchUnit llegan en el 1.4). Se trabaja en una rama y entra por pull request.
*Terminado cuando:* arranca (sin base de datos) y `/actuator/health` responde.

**1.2 Docker Compose local** con PostgreSQL y Floci (verificar nombre y versión de la imagen en su README y fijarla), leyendo las variables del `.env`. La app todavía no se conecta a la base de datos.
*Terminado cuando:* `docker compose up` levanta PostgreSQL y Floci, y puedes conectarte a ambos (a PostgreSQL con `psql` o un cliente, y a Floci con la AWS CLI apuntando a `localhost:4566`).

**1.3 JPA, Flyway y primera migración.** Agregar Data JPA, PostgreSQL y Flyway al proyecto, configurar el perfil `local` contra el compose del paso 1.2 y escribir la primera migración, solo con `product` y `delivery_slot` y la restricción `CHECK (booked <= capacity)`.
*Terminado cuando:* al arrancar, Flyway crea las tablas y el health muestra la base de datos conectada.

**1.4 Primeras pruebas.** Agregar las dependencias de Testcontainers y ArchUnit. Una unitaria con JUnit 5 de algo trivial, una de integración con Testcontainers que levanta PostgreSQL y verifica que las migraciones corren, y una de arquitectura con ArchUnit.
*Terminado cuando:* `./mvnw verify` pasa en tu máquina.

**1.5 CI.** El workflow `ci` en cada PR: detección de cambios, compilar, `verify` y el job final `ci-ok`. Después, marcar `ci-ok` como check obligatorio en la protección de `main`.
*Terminado cuando:* un PR muestra `ci-ok` en verde y no se puede mergear sin él.

**1.6 Dockerfile y proxy.** Dockerfile del backend en multietapa y con usuario no root, la carpeta `proxy/` con la configuración de Nginx, y Nginx en el compose como único punto de entrada (`/api/v1` hacia el backend, sin exponerlo directamente). Actuator queda solo en la red interna, y la API expone un endpoint de salud mínimo bajo `/api/v1` para los chequeos externos.
*Terminado cuando:* la imagen construye y el endpoint de salud responde a través de Nginx en `http://localhost/api/v1/...`.

**Cierre de la fase 1:** backend mínimo con pruebas, CI y contenedor, todo local y sin cuenta de AWS.

**Pasos de AWS (se hacen en las fases 8 y 9, no ahora):**
- Usuario IAM con MFA, alerta de presupuesto (por ejemplo, US$10) y plan de pago.
- Bucket del estado de Terraform y Terraform: ECR, EC2 pequeña con Docker, grupo de seguridad (puertos 80 y 443, SSH solo desde tu IP) e IP elástica. Antes se ensaya contra Floci (fase 7).
- Dominio apuntando a la IP elástica, Nginx o Caddy con Let's Encrypt.
- Despliegue por pipeline con OIDC.
- *Terminado cuando:* un merge a `main` se ve en producción con HTTPS sin tocar la EC2 a mano.

---

## Anexo B: negocio y marketing (aparte del proyecto técnico)

Solo aplica si ella decide usar la tienda de verdad.

**Antes de vender:**
- Definir capacidad por día, zonas y costo de envío. Ella trabaja sola y tiene otro trabajo: usar pedidos con 24 a 48 horas de anticipación, cupos y ventanas de entrega fijas.
- Acuerdo escrito corto (ver sección 14).
- Formalización en Colombia: RUT, matrícula mercantil, facturación electrónica (DIAN), Estatuto del Consumidor (Ley 1480, incluido el retracto), protección de datos (Ley 1581) y términos y condiciones. Confirmar con un contador y un abogado.
- Pagos en producción: cuenta de comercio y de banco a nombre de ella.
- Medios de pago y su parte legal: informar con claridad los medios aceptados y el precio total (con el envío), facturar sin importar el medio de pago, verificar las transferencias en la app del banco y no por capturas, llevar un registro de cada cobro en efectivo y cuidar la seguridad de quien entrega. Confirmar todo con un contador y un abogado.

**Marketing (por orden):**
1. Fotos reales de cada arreglo (una foto por pedido real, o una sesión corta).
2. Google Business Profile (lo que más trae clientes locales en Bogotá).
3. Instagram, TikTok y WhatsApp Business con catálogo.
4. Reseñas desde el primer pedido.
5. Anuncios pequeños en Meta hacia WhatsApp, en 2 o 3 zonas (por ejemplo Chapinero, Usaquén, Cedritos, Chicó, Teusaquillo), solo cuando ella pueda atender.
6. Campañas por fecha: diciembre, San Valentín y Día de la Madre, con 2 a 3 semanas de anticipación.

**SEO:** SEO local primero, luego palabras clave ("flores a domicilio Bogotá", "arreglos florales Bogotá"), una página por necesidad con textos y fotos propias, y medición con Search Console y GA4. Los resultados tardan de 6 a 12 meses.

**Medición semanal:** mensajes recibidos, costo por mensaje, pedidos cerrados, ticket promedio y reseñas nuevas.

**Aviso:** nadie puede garantizar resultados de publicidad; es casi seguro que se necesita invertir, pero conviene probar con poco presupuesto y medir.

---

## Anexo C: glosario

- **Pull request (PR):** una propuesta de cambio. Trabajas en una rama aparte y pides que se mezcle con `main`.
- **CI (integración continua):** pruebas automáticas que GitHub corre cada vez que abres un PR.
- **Workflow, job y check:** el *workflow* es el guion automático de GitHub Actions, un *job* es una tarea dentro de él y un *check* es el resultado (verde o rojo) que ves en el PR.
- **`ci-ok`:** un job final que corre siempre y resume a los demás. Se marca como el único check obligatorio, para que un PR no quede bloqueado esperando una prueba que no corrió porque no había cambios en esa carpeta.
- **Filtro de rutas:** hace que un workflow corra solo si cambiaron ciertas carpetas (por ejemplo, `backend/**`).
- **E2E (extremo a extremo):** un robot con navegador (Playwright) que hace lo que haría un cliente: abre la página, elige un arreglo y una franja y paga en sandbox. Corre contra todo el sistema porque los errores aparecen en la unión del frontend con el backend.
- **ADR:** documento corto que explica una decisión de arquitectura y por qué se tomó.
- **`.tfvars`:** archivo con los valores reales que usa Terraform (región, tamaño de la máquina, dominio). El código de `infra/` declara variables genéricas y el `.tfvars` las rellena. Los reales no se suben al repo; se sube un `.tfvars.example`.
- **SSM Parameter Store y Secrets Manager:** los dos servicios de AWS para guardar secretos (llaves, contraseñas) fuera del código. El primero es gratis en su nivel estándar; el segundo cobra por secreto al mes y ofrece rotación automática.
- **OIDC:** permite que GitHub entre a AWS sin guardar una contraseña ni una llave permanente.
- **Puerto y adaptador:** el puerto es una interfaz que define el negocio ("necesito cobrar") y el adaptador es la pieza que la cumple con una tecnología concreta (Mercado Pago, S3).
- **Webhook:** un aviso que el proveedor de pagos le envía a tu servidor cuando algo ocurre (por ejemplo, un pago aprobado).
- **Idempotencia:** que procesar el mismo evento dos veces tenga el mismo efecto que procesarlo una.
- **Sandbox:** entorno de pruebas de la pasarela, con dinero de mentira.
- **Floci:** emulador de AWS que corre en un contenedor, para desarrollar sin cuenta real.
- **Testcontainers:** librería que levanta contenedores reales (como PostgreSQL) durante las pruebas y los destruye al terminar.
- **Flyway:** herramienta que aplica los cambios de la base de datos como migraciones versionadas.
- **PSE:** transferencia bancaria en línea. **Efecty:** red de puntos donde se paga en efectivo un comprobante generado en la tienda.
