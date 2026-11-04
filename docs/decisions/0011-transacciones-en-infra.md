# 0011. Las transacciones se declaran en `infra`

**Estado:** aceptada · **Fecha:** 2026-11-04

## Contexto
La reserva de cupo usa bloqueo pesimista de fila (`SELECT ... FOR UPDATE`), que solo funciona dentro de una transacción. Los servicios de `application` no pueden depender de Spring (ADR 0001, regla 2), así que no pueden llevar `@Transactional`.

## Decisión
- Los servicios de `application` siguen siendo Java puro.
- Cuando un caso de uso necesite transacción, una subclase en `infra/config` sobrescribe el método y le añade `@Transactional`, sin más lógica.
- Esa subclase se registra como bean en la clase de configuración del módulo. Los controladores inyectan la clase base y reciben la transaccional.

## Consecuencias
- **A favor:** `application` sigue sin Spring y ArchUnit no necesita excepciones. Una sola técnica de cableado.
- **En contra:** una clase de unas diez líneas por cada caso de uso que necesite transacción.
- **Se revisa en la fase 3:** si crear un pedido y reservar cupo deben deshacerse juntos, la transacción tendrá que abarcar ambos pasos, y habrá que decidir dónde nace.
- **Descartado:** `@Transactional` directo en `application` (rompe la pureza) y un puerto de transacciones (más verboso para este tamaño).