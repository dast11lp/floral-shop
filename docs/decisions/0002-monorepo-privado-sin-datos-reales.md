# 0002. Monorepo privado, sin datos reales

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
El proyecto es de portafolio, pero puede llegar a usarse en un negocio real. Backend, frontend e infraestructura cambian juntos con frecuencia, y quien clone el repo debería poder levantarlo con un comando.

## Decisión
Un solo repositorio con `backend/`, `frontend/`, `infra/`, `docs/` y `.github/workflows/`, **privado** hasta que se decida publicarlo.

Reglas:
- Nada real dentro del repositorio: sin secretos, sin IDs de cuenta ni de pasarela, sin datos ni fotos reales. Los datos de ejemplo son inventados.
- Los valores reales viven en variables de entorno, secretos de GitHub y SSM o Secrets Manager. Un `.env.example` documenta qué variables existen.
- `infra/` es genérico (módulos y variables); los `.tfvars` reales no se commitean.
- `main` protegida, todo por pull request.

## Consecuencias
- **A favor:** un cambio que toca varias partes va en un solo commit, un solo CI, y el repo se publica limpio cuando llegue el momento. Si ella necesita acceso solo a la infraestructura, `infra/` se puede extraer a otro repo sin reescribir.
- **En contra:** exige disciplina permanente para no meter nada real; un secreto que se cuele queda en el historial.
- **Descartado:** repos separados (más configuración para una sola persona), carpeta `apps/` (sin beneficio con dos aplicaciones) y carpeta `deploy/` (mezcla ejecución local con infraestructura en la nube).
