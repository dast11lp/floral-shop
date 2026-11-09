# 0010. Estructura de paquetes por módulo y ubicación de los puertos (enmienda del 0001 y el 0009)

**Estado:** aceptada · **Fecha:** 2026-11-03

## Contexto
El ADR 0001 ponía los puertos (interfaces) en `domain` y no definía subestructura dentro de `infra`. Al construir la persistencia de `scheduling` (2.4a) se vio que la diferencia entre adaptadores de entrada y de salida no era evidente en el código. Se adopta una estructura que hace visible esa dirección.

## Decisión
Cada módulo se organiza así:

```
<módulo>/
├── domain/
│   ├── model/         modelos de negocio
│   └── exception/     excepciones de negocio
├── application/
│   ├── port/out/      puertos de salida: lo que el negocio necesita del exterior
│   └── service/       casos de uso y reglas de negocio
└── infra/
    ├── adapter/
    │   ├── in/web/            controladores REST, requests y responses
    │   └── out/persistence/   entidad JPA, repositorio de Spring Data, mapper y adaptador
    └── config/        registro de beans
```

- Los puertos de salida viven en `application/port/out`. `domain` no contiene interfaces.
- No hay puertos de entrada (interfaces `UseCase`): los controladores usan la clase del servicio directamente, como establece la sección 3 del plan.
- Nombres: `<Concepto>RepositoryPort`, `<Concepto>PersistenceAdapter`, `<Concepto>PersistenceMapper`, `<Concepto>JpaEntity` y `<Concepto>JpaRepository`.
- Los servicios de `application` no llevan anotaciones de Spring: se registran como beans en `infra/config`.
- Se mantiene el nombre `infra` (y no `infrastructure`), porque las reglas de ArchUnit usan el patrón `..infra..`.
- Las seis reglas de dependencia del ADR 0001 no cambian. `infra` implementa los puertos de `application`, y eso ya lo permite la regla 3.

## Consecuencias
- **A favor:** la dirección entrada/salida se ve en las carpetas; `application` declara lo que necesita del exterior; la estructura es reconocible para quien conozca el patrón.
- **En contra:** más carpetas, algunas con pocas clases.
- **Se aplica a:** los módulos nuevos y a `scheduling`. El paquete `health`, que tiene una sola clase, se deja como está hasta que crezca.

## Precisiones (módulo `catalog`, 2.5a)
- Los servicios de solo lectura y sin reglas de negocio pueden agruparse en un único servicio por módulo (`CatalogQueryService`). "Un servicio por caso de uso" (ADR 0009) aplica a los servicios que tienen reglas.
- La paginación usa tipos propios (`PageQuery` y `PagedResult`), ubicados en `domain/model` de `catalog`, porque `application` no puede depender de los tipos de Spring Data. Si otro módulo los necesita, se moverán a `shared` y se ajustarán las reglas de ArchUnit.
- **API pública de un módulo (regla 4 del ADR 0001):** lo que otro módulo puede usar es `application/service` (los casos de uso) y `domain` (los modelos y excepciones que esos casos de uso devuelven o lanzan). Son internos, y ningún otro módulo debe tocarlos, `infra` y `application/port`. ArchUnit lo comprueba descubriendo los módulos por sus paquetes.
- **Regla 5 (cada módulo es dueño de sus tablas):** ArchUnit comprueba que las entidades JPA solo viven en `infra/adapter/out/persistence` y que los repositorios de Spring Data no son públicos. No puede ver consultas SQL escritas como texto ni claves foráneas entre tablas de módulos distintos: se revisan a mano cuando aparezca la primera relación entre módulos (fase 3).
- **Errores HTTP:** un `GlobalExceptionHandler` en `shared/infra/web`, con la menor prioridad, da formato `ProblemDetail` a los errores estándar de Spring y atrapa lo imprevisto con un 500 genérico (el detalle va al log, no al cliente). Cada módulo con controladores tiene su propio `<Módulo>ExceptionHandler`, con la mayor prioridad, que solo traduce las excepciones de dominio de ese módulo.
- **Respuestas de la API:** `records` `<Concepto>Response` en `infra/adapter/in/web`, con un método estático `from(...)`. No exponen ids internos, `active` ni `sortOrder`. Las variantes sí exponen su id, porque los pedidos las referenciarán (fase 3).
- **Documentación OpenAPI:** springdoc, activada solo con el perfil `local`. En producción está desactivada, y además Nginx solo deja pasar `/api/v1`.