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