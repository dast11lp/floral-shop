# 0014. Pagos con varias pasarelas, avisos entre módulos y correo sin atarse a un proveedor (enmienda del 0003, 0004, 0012 y 0013)

**Estado:** aceptada; con dos precisiones (2026-11-12): Mercado Pago con Checkout Pro, y el inicio del pago en tres pasos · **Fecha:** 2026-11-11

## Contexto
Antes de programar los pagos de la fase 3 se revisaron cinco preguntas:

1. **Varias pasarelas a la vez.** No basta con poder cambiar de pasarela: se quiere tener el código de varias (Mercado Pago, Wompi...), encender las que se quieran y que el cliente elija.
2. **Cómo se entera el pedido del resultado del pago.** El ADR 0013 (punto 4) decidió llamadas directas entre módulos "hasta que lleguen los correos o si los pagos lo exigen".
3. **El correo se va a hacer.** Un correo enviado no se puede deshacer, así que no puede tratarse como un paso más de la transacción.
4. **No depender de Amazon.** El plan proponía SQS y SES para los correos (fase 6). Se quiere poder pasar a una VPS sin reescribir el negocio.
5. **Control y simplicidad.** Cada PR se lee y se aprueba a mano, así que todo lo que no sea indispensable se deja fuera.

## Decisión

**1. Varias pasarelas encendidas a la vez; el cliente elige la combinación.**
- La política de pago (`PaymentMethodPolicy`, ADR 0003) pasa a ser **por pasarela y medio**, no solo por medio. Cada fila dice si esa combinación está habilitada, cuántos minutos se aparta el cupo y su **prioridad**. Ejemplo: Mercado Pago + tarjeta, Mercado Pago + PSE, Wompi + Nequi.
- **La sugerida al arrancar es Mercado Pago + tarjeta.** Es también la primera que se construye y se prueba en el sandbox.
- La tienda muestra las combinaciones habilitadas ordenadas por prioridad. **La primera es la sugerida** y se muestra en grande; las demás, en pequeño. Si se apaga la sugerida, la siguiente toma su lugar sola.
- Cada pago guarda su pasarela. Su consulta de estado y sus avisos van siempre a esa pasarela, así que cambiar las combinaciones no rompe los pagos en curso.
- Se mantiene el registro por proveedor del ADR 0003: cada pasarela es un adaptador en `infra`, y el adaptador traduce el idioma del proveedor al nuestro (capa anticorrupción).
- El puerto `PaymentGateway` se diseña según lo que necesita la tienda, no según la API de Mercado Pago. Crece solo cuando una pasarela real lo exija.
- El primer adaptador es Mercado Pago. Agregar otro es escribir su clase y agregar sus filas en la política, sin tocar el resto.

**2. Las credenciales de las pasarelas viven en la configuración del servidor** (ADR 0006), no en la base de datos.
- La consola de administración muestra si cada pasarela tiene credenciales ("listas" o "faltan"), pero no las escribe ni las muestra.
- **Regla:** no se puede habilitar una combinación si su pasarela no tiene credenciales. Se revisa al habilitarla y también al armar las opciones para el cliente, así que si las credenciales desaparecen, la opción desaparece sola.
- La regla pregunta por las credenciales a través de un puerto. Si algún día las credenciales pasan a la base de datos, solo cambia el adaptador de ese puerto.
- **Descartado por ahora:** que la florista escriba las credenciales en la consola. Si alguien roba la sesión de administración, podría poner las de su propia cuenta y desviar los pagos. Si se hace algún día, tiene que ser con las credenciales cifradas (la llave fuera de la base de datos), sin volver a mostrarlas, pidiendo la contraseña de nuevo para cambiarlas, con un aviso por correo y un registro de quién lo hizo.
- Mientras la consola no exista (fase 4), habilitar una combinación es un cambio de datos en la tabla.

**3. Un pago fallido cancela el pedido; el carrito se conserva.**
- Si el pago se rechaza o vence, el pedido se cancela y se devuelve su lugar en la franja.
- El carrito vive en el navegador, así que no se borra. Reintentar crea un pedido nuevo con lo mismo. Si la franja ya se llenó, el cliente elige otra.
- Un pedido nunca tiene dos pagos activos a la vez.

**4. Avisos entre módulos: llamada directa para lo que debe pasar junto, eventos para lo que sale de la aplicación.**

Hay dos tipos de reacciones a un pago:
- **Tipo 1, todo o nada, dentro de nuestra base de datos:** el pago queda aprobado y el pedido confirmado; o el pago queda rechazado, el pedido cancelado y el lugar en la franja devuelto.
- **Tipo 2, efectos hacia afuera que no se pueden deshacer:** el correo al cliente, el aviso a la florista y el comprobante en PDF.

Cómo se resuelve cada uno:
- **El tipo 1 va con llamadas directas, en una sola transacción.** Pagos llama a un servicio público de pedidos con dos operaciones (confirmar por pago aprobado y cancelar por pago fallido). Pedidos llama a franjas para devolver el lugar. Pagos depende de pedidos y pedidos de franjas, sin ciclos. Si un paso falla, no queda ninguno, y la pasarela reintenta el aviso después.
- **El tipo 2 nunca va dentro de la transacción.** Si el correo saliera y la transacción fallara después, el cliente recibiría "tu pedido está confirmado" de un pedido que no se confirmó. Además, un servidor de correo lento dejaría bloqueada la fila del pago.
- **Para el tipo 2, pedidos publica eventos**, por ejemplo "pedido confirmado" y "pedido cancelado". Los publica pedidos y no pagos, porque son hechos del pedido. Pagos no sabe que el correo existe.
- **Los eventos no se pueden perder: bandeja de salida (Transactional Outbox).** El evento se guarda en una tabla dentro de la misma transacción que el pedido, y después se entrega con reintentos hasta lograrlo.
- **Quien recibe un evento debe tolerar recibirlo dos veces** (consumidor idempotente). Por ejemplo, antes de mandar el correo revisa si ya se mandó.
- **Para que el flujo no se esconda,** los eventos son records en un paquete conocido, con nombres en pasado. Hay un escuchador por reacción, con un nombre que diga lo que hace, y una prueba que recorre el flujo completo, desde el pago hasta el correo.
- **Cómo se implementa la bandeja de salida se decide al construir el correo.** Las opciones son Spring Modulith (que la trae hecha), una tabla y un job propios, o solo "después de guardar" con un botón para reenviar el correo. Los PR de pagos no la necesitan.
- Esto actualiza el punto 4 del ADR 0013: el tipo 1 sigue sin eventos, y los eventos entran solo para el tipo 2.

**5. Un aviso repetido se reconoce por el estado del pago.**
- Las pasarelas pueden mandar el mismo aviso varias veces. Si el pago ya no está pendiente, el aviso ya se procesó y se ignora.
- Si llegan dos avisos al mismo tiempo, el pago se lee con bloqueo de fila (`SELECT ... FOR UPDATE`), como las franjas, así que el segundo espera y luego ve el estado nuevo.
- El estado nunca se toma del cuerpo del aviso: siempre se consulta a la pasarela (ADR 0003). Por eso, un aviso viejo o desordenado no puede retroceder un pago.
- Reemplaza la tabla de eventos procesados con restricción única `(provider, event_id)` del ADR 0003.

**6. Correo por SMTP, con la cola en PostgreSQL, sin SQS ni SES.**
- Hay un solo adaptador de `EmailSender`, que usa SMTP (`JavaMailSender` de Spring). SMTP es un estándar, así que cambiar de proveedor es cambiar el servidor, el usuario y la clave en la configuración.
- Tres configuraciones, con el mismo código:
  - **Local:** Mailpit en Docker. Atrapa los correos para verlos en el navegador y no envía nada real.
  - **Demostración para la florista:** Gmail con una contraseña de aplicación, solo para que ella vea un correo real. No es para producción.
  - **Producción:** Brevo (300 correos al día gratis), con Resend como plan B.
- Producción necesita un dominio propio, con SPF, DKIM y DMARC, para que los correos no lleguen a spam.
- No hace falta SQS: la bandeja de salida del punto 4 ya funciona como una cola durable dentro de PostgreSQL.
- Floci deja de usarse para el correo (ADR 0004). Sigue sirviendo para S3 y los secretos mientras se use AWS.

**7. Ninguna librería de un proveedor sale de su adaptador.**
- Una regla de ArchUnit prohíbe usar librerías de Amazon (`software.amazon..`) o de Mercado Pago (`com.mercadopago..`) fuera de los adaptadores de `infra`.
- Así, pasar a una VPS es configuración y, como mucho, un adaptador (`plan.md`, sección de migración). Terraform y SSM (ADR 0005 y 0006) siguen siendo de AWS. Si la VPS pasa a ser el camino principal, eso es otra decisión.

**8. Sin pasarela falsa.**
- Se prueba con el sandbox de Mercado Pago.
- En las pruebas unitarias, el puerto se simula con Mockito.
- El adaptador se prueba contra WireMock, con una suite de contrato que reutilizará cada adaptador nuevo.

## Consecuencias
- **A favor:**
  - Se pueden ofrecer varias pasarelas a la vez y encenderlas sin programar.
  - Lo que debe pasar junto pasa junto, y un correo nunca anuncia algo que no ocurrió.
  - El correo no depende de un proveedor ni de Amazon, y es gratis al volumen de la tienda.
  - Hay una tabla menos (la de eventos procesados) y no hay pasarela falsa.
- **En contra:**
  - Pagos queda acoplado a pedidos (dos operaciones, vigiladas por ArchUnit).
  - La bandeja de salida es una pieza más cuando llegue el correo.
  - Las credenciales solo las cambia quien administra el servidor, no la florista.
  - Probar el aviso de la pasarela en el PC exige un túnel (ngrok o similar), porque la pasarela no puede llegar a `localhost`.
- **Enmienda al 0003:**
  - La política pasa a ser por pasarela y medio, con prioridad.
  - Los avisos repetidos se reconocen por el estado del pago y no por una tabla de eventos.
  - No hay `FakePaymentGateway`.
  - El resto del 0003 no cambia: el puerto, el registro por proveedor, la firma del webhook y la consulta del estado a la pasarela.
- **Enmienda al 0004:** Floci ya no se usa para el correo (SQS y SES).
- **Enmienda al 0012** (punto 11): el correo va por SMTP y su cola es la bandeja de salida, no SQS a SES.
- **Enmienda al 0013:**
  - Punto 4: los eventos entran solo para los efectos hacia afuera.
  - Punto 5: el tiempo de cupo apartado se guarda por pasarela y medio, no solo por medio. El valor (36 minutos) y sus fuentes no cambian.

## Pendientes
- Explicarle a la florista por qué Gmail no sirve en producción: el remitente sería un Gmail personal, tiene límites y no está pensado para una tienda.
- Elegir el dominio de la tienda, que el correo de producción necesita.
- Confirmar en las páginas oficiales los límites gratuitos de Brevo y Resend, y que Gmail siga ofreciendo contraseñas de aplicación, cuando se configure el correo.
- Elegir la implementación de la bandeja de salida al construir el correo. Si es Spring Modulith, verificar que su versión sea compatible con Spring Boot 4.1.
- Confirmar que la cuenta de sandbox de Mercado Pago sigue activa y sacar sus credenciales de prueba.

## Precisión (2026-11-12): Mercado Pago con Checkout Pro vía la API de Orders
Al construir el inicio del pago (PR 1b) se eligió cómo hablar con Mercado Pago y se comprobó en el sandbox. El texto de arriba no cambia.
- **Checkout Pro vía la API de Orders**, no vía preferencias. El panel de Colombia la ofrece al crear la aplicación y Mercado Pago la recomienda para integraciones nuevas. El cobro se crea con `POST /v1/orders` y el cliente va a su `checkout_url`.
- **El cobro vence a los 15 minutos** con `expiration_time` (`PT15M`), que reemplaza al `expiration_date_to` de las preferencias que citaba el ADR 0013. Los 36 minutos de cupo no cambian.
- **En pesos colombianos los montos van enteros** ("145000", no "145000.00"); con decimales, Mercado Pago responde 400.
- **Cada opción excluye los demás tipos de pago** (`not_allowed_types`). Con tarjeta se excluyen `bank_transfer`, `ticket` y `account_money`, y la página de Mercado Pago solo muestra "Tarjeta (crédito, débito o prepago)". Con PSE se excluyen las tarjetas, `ticket` y `account_money`.
- **Nequi sí aparece como banco dentro de PSE** en Mercado Pago (consulta de `GET /v1/payment_methods` de la cuenta de prueba). Resuelve el punto que el ADR 0013 dejaba por verificar.
- **Sin SDK de Mercado Pago:** el adaptador usa el `RestClient` de Spring, con HTTP/1.1 y tiempos máximos (5 segundos para conectar y 15 para responder).

## Precisión (2026-11-12): iniciar el pago en tres pasos
Al revisar el PR 1b antes de fusionarlo se ajustó cómo se inicia el pago, para que la llamada a la pasarela también cumpla el punto 4: lo que sale de la aplicación no va dentro de la transacción. El texto de arriba no cambia.
- **Tres pasos, con la pasarela fuera de toda transacción:**
  1. Guardar el intento de pago (pendiente, todavía sin cobro) y extender el vencimiento del cupo del pedido (ADR 0013, precisión del 2026-11-12). Es una transacción corta.
  2. Crear el cobro en la pasarela, sin transacción abierta. Así una pasarela lenta no deja una conexión ni una fila bloqueadas.
  3. Anotar el cobro (su id y su URL), o marcar el intento `FAILED` si algo falló. Es otra transacción corta. Un intento fallido no impide reintentar.
- **Dos peticiones a la vez para el mismo pedido:** un índice único deja un solo pago pendiente por pedido (punto 3). La segunda petición choca al guardar, antes de llamar a la pasarela, y recibe 409 ("ya hay un pago en curso"), no un error interno.
- **Llave de idempotencia:** el código público del pedido más el id del intento. Si la red repite la llamada del mismo intento, no se crean dos cobros. Y es única aunque la base de datos se recree y los ids vuelvan a empezar.
- **Cambiar de opción con un pago en curso** responde 409 por ahora, con un mensaje que pide terminar ese pago o esperar a que venza. Hacerlo bien exige consultar y cancelar el cobro en la pasarela, y eso llega con el PR 2. La espera está acotada por el tope del cupo (51 minutos).

## Fuentes del punto 6
Consultadas el 2026-11-11. En parte son de terceros; se confirman al configurar.
- Brevo, API y SMTP con 300 correos al día gratis: https://www.brevo.com/features/email-api/
- Resend, plan gratis de 3.000 al mes y 100 al día: https://www.resend.com/docs/knowledge-base/what-is-resend-pricing.md
- Amazon SES, precios (0,10 USD por cada 1.000; las cuentas nuevas reciben créditos en vez del plan gratis fijo): https://aws.amazon.com/en/ses/pricing/
- SendGrid terminó su plan gratis: https://www.smtp2go.com/blog/sendgrid-has-ended-its-free-plan-we-have-got-you-covered/
