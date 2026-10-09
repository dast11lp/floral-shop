# 0002. Monorepo público, sin datos reales

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
El proyecto es de portafolio, pero puede llegar a usarse en un negocio real. Backend, frontend e infraestructura cambian juntos con frecuencia, y quien clone el repo debería poder levantarlo con un comando.

Se empezó con un repositorio privado. Al probarlo, se creó una regla de protección de `main` (todo por pull request, sin borrados ni force push) y un push directo a `main` **no fue rechazado**, dos veces. En el plan gratuito de GitHub, la protección de ramas y el escaneo de secretos con bloqueo de pushes solo se aplican en repositorios públicos.

## Decisión
Un solo repositorio con `backend/`, `frontend/`, `infra/`, `docs/` y `.github/workflows/`, **público desde el inicio**, con una licencia explícita (ver la decisión 0008).

Reglas:
- Nada real dentro del repositorio: sin secretos, sin IDs de cuenta ni de pasarela, sin datos ni fotos reales. Los datos de ejemplo son inventados.
- Los valores reales viven en variables de entorno, secretos de GitHub y SSM Parameter Store. Un `.env.example` documenta qué variables existen.
- `infra/` es genérico (módulos y variables); los `.tfvars` reales no se commitean.
- `main` protegida, todo por pull request, con el check `ci-ok` obligatorio.
- Escaneo de secretos y bloqueo de pushes con secretos activados, más `gitleaks` en el CI.

## Consecuencias
- **A favor:** la protección de `main` y el escaneo de secretos funcionan y son gratuitos, el historial de commits, PRs y pipeline sirve de portafolio, y un cambio que toca varias partes va en un solo commit.
- **En contra:** el trabajo en curso es visible, y cualquier secreto o dato real que se cuele queda público de inmediato (y en el historial). Exige disciplina permanente y revisar cada commit.
- **Mitigación:** la regla de "nada real", el escaneo de secretos de GitHub, `gitleaks` en el CI y una revisión del historial antes de cada cambio de visibilidad.
- **Descartado:** repositorio privado (la protección no se aplicó en el plan gratuito), GitHub Pro (de pago), repos separados (más configuración para una sola persona), carpeta `apps/` (sin beneficio con dos aplicaciones) y carpeta `deploy/` (mezcla ejecución local con infraestructura en la nube).
