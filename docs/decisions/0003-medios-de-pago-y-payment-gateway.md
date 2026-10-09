# 0003. Medios de pago y `PaymentGateway` como puerto y adaptador

**Estado:** aceptada, con puntos pendientes de verificar · **Fecha:** 2026-10-08

## Contexto
En Colombia mucha gente paga con PSE, Nequi, Daviplata o efectivo, y no solo con tarjeta. Un checkout que no ofrece el medio del cliente pierde la venta. Además, el negocio es una persona sola que vende un producto perecedero, hecho por encargo y con entrega programada, así que cada medio de pago implica un riesgo distinto (por ejemplo, un cliente que rechaza el pedido en efectivo).

La pasarela inicial es Mercado Pago, pero puede cambiar (comisiones, tiempos de acreditación, requisitos legales). Los webhooks son un punto sensible: pueden llegar duplicados, desordenados o falsificados.

## Decisión
**1. Se separa "cómo paga el cliente" de "quién procesa el pago".**
- Medio de pago (`PaymentMethod`): tarjeta, PSE, Nequi, efectivo en un punto de pago (Efecty), transferencia manual y contra entrega.
- Proveedor (`provider`): Mercado Pago, Wompi o `manual`.
- Cada medio tiene una política (`PaymentMethodPolicy`): si está habilitado, cómo se confirma, cuánto tiempo se retiene el cupo y qué límites tiene (monto máximo, zonas, anticipación mínima).

**2. `PaymentGateway` es el puerto de los proveedores en línea:**

```java
public interface PaymentGateway {
    String provider();
    CheckoutSession createCheckout(PaymentRequest req);
    Optional<PaymentEvent> parseWebhook(RawWebhook raw);
    PaymentStatus fetchStatus(String providerPaymentId);
}
```
- Cada proveedor es un adaptador en `infra`. Mercado Pago primero; Wompi como segundo adaptador, recomendado si se quiere Nequi nativo.
- Registro por proveedor (`Map<String, PaymentGateway>`) y capa anticorrupción que traduce el vocabulario del proveedor a `PaymentStatus` propio.
- Se guarda `provider` y `method` en cada pago, y los eventos se deduplican con restricción única `(provider, event_id)`.
- El webhook se valida por firma y el estado se reconfirma consultando la API del proveedor. Un pedido pasa a pagado solo por esa vía.

**3. Medios que no pasan por una pasarela** son casos de uso propios, con el mismo ciclo de estados (`PENDING`, `PAID`, `FAILED`, `EXPIRED`, `CANCELLED`) y los mismos controles:
- **Transferencia manual** (Nequi o cuenta de ella): la confirma solo el admin, tras verificarla en la app del banco, nunca por captura de pantalla. Queda registrado quién y cuándo, y confirmarla dos veces no la duplica.
- **Contra entrega:** **deshabilitado por defecto.** Si se habilita (decisión con ella), va con límites de monto y zonas, y con confirmación previa. El cobro se registra al entregar.

**4. Retención del cupo según el medio:** 10 a 15 minutos en pagos en línea; hasta el vencimiento del comprobante en efectivo en un punto de pago (sin pasar del límite de anticipación de la entrega); configurable en transferencia manual; inmediata en contra entrega.

**5. Pruebas:** suite de contrato compartida contra `FakePaymentGateway` y contra el adaptador (con WireMock), y pruebas de la máquina de estados y de la política de cada medio.

## Consecuencias
- **A favor:** se pueden ofrecer los medios que los clientes usan sin acoplar el negocio a un proveedor, y cambiar de pasarela es escribir un adaptador.
- **En contra:** más casos de uso y más estados que probar, y la confirmación manual añade trabajo operativo a una persona que ya está saturada.
- **Aspectos legales a confirmar con un contador y un abogado:** facturación electrónica sin importar el medio de pago; información clara de los medios aceptados y del precio total (Estatuto del Consumidor, Ley 1480), incluido el derecho de retracto y los reembolsos; datos personales que puedan aparecer en los comprobantes (Ley 1581); condiciones de la cuenta de ella para recibir pagos de un negocio; y registro y seguridad del efectivo.

## Pendientes
- Qué medios ofrece de verdad cada proveedor. Según la documentación revisada, Wompi lista tarjetas, PSE, Nequi, Daviplata y opciones de Bancolombia; Mercado Pago en Colombia ofrece en Checkout Pro tarjetas, saldo de la cuenta y PSE o Efecty, y Nequi pasaría por PSE. Confirmarlo en el sandbox.
- Comisiones y tiempos de acreditación de cada proveedor.
- Si Mercado Pago o Wompi exigen constitución legal para las llaves de producción.
- Decidir con ella si se habilita el contra entrega y con qué límites.
