# 0008. Propiedad del código y licencia

**Estado:** propuesta (pendiente de acordar con ella) · **Fecha:** 2026-10-08

## Contexto
Si la florista usa la tienda, será desplegada en su cuenta de AWS, pero el código lo escribe otra persona. Conviene dejar claro de quién es qué antes del despliegue. Esto no es asesoría legal: se debe confirmar con un abogado.

## Decisión
- **Tú mantienes el código y ella tiene una licencia de uso** para su negocio. El repositorio sigue en tu cuenta de GitHub, y ella usa la tienda desplegada y su panel admin, sin tocar el repo.
- En Colombia, el software se protege por regla general por **derecho de autor**, no por patente. Nace al crear la obra, sin trámite. El registro ante la Dirección Nacional de Derecho de Autor es voluntario.
- El repositorio es privado hasta decidir publicarlo. Antes de publicarlo se elige una **licencia explícita**, porque sin licencia nadie puede reutilizarlo legalmente.
- Un **acuerdo corto con ella**, aunque sea un mensaje aclarado, que cubra: el código es tuyo y ella tiene licencia; de quién son el dominio, el Instagram y las cuentas; qué pasa si cada uno sigue por su lado; porcentaje; y quién factura.
- Lo que no es tuyo es su marca, sus fotos y su catálogo.
- Se hace **antes de la fase 8**, porque el despliegue ocurre en su cuenta.

## Consecuencias
- **A favor:** como el repo no tiene nada real, cualquier traspaso futuro (transferir el repo, entregar una copia o incorporar a otro desarrollador) es limpio: se cambia el dueño y los secretos.
- **En contra:** exige tener una conversación incómoda a tiempo.
- **Alternativas:** transferirle el repo (si dejas de involucrarte), darle una copia o fork (se desactualiza) o abrirle acceso a otro desarrollador.
