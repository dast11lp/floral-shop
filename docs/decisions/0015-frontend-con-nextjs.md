# 0015. Frontend con Next.js en vez de Angular

**Estado:** aceptada · **Fecha:** 2026-11-12

## Contexto
El plan eligió Angular con SSR para el frontend (decisión 10 de `plan.md`) y dejó Next.js como alternativa. El frontend todavía no existe: llega en la fase 5, así que cambiar ahora no cuesta código.

Al revisar la decisión pesaron cuatro cosas:
- **Trabajo:** React tiene más ofertas que Angular, y Next.js es su framework más usado. El proyecto también es una pieza de portafolio.
- **IA:** las herramientas de IA generan mejor React y Next.js, porque hay mucho más código de ejemplo.
- **SEO del catálogo:** renderizar en el servidor (SSR), generar páginas estáticas o regenerarlas cada cierto tiempo es el fuerte de Next.js.
- **Experiencia previa:** se conoce Vite y React, y Next.js se usó hace años.

En rendimiento no hay diferencia que importe para una floristería.

## Decisión
- **Next.js con React y TypeScript**, con el App Router. Reemplaza a Angular en la fase 5.
- **Next.js es solo la cara de la tienda.** Las reglas de negocio siguen en Spring Boot. El código de servidor de Next.js solo renderiza páginas y llama a la API (`/api/v1`). No toca la base de datos ni decide nada de pedidos, cupos o pagos. Así, cambiar de framework nunca toca el negocio.
- **Sin depender de Vercel:** se despliega en Docker con `output: "standalone"`, detrás de Nginx, como estaba planeado (`/` va a Next.js y `/api/v1` a Spring Boot). Funciona igual en AWS o en una VPS. El ADR 0007 no cambia: siguen siendo dos imágenes y dos despliegues independientes.
- **Qué se renderiza dónde:**
  - Inicio, catálogo y producto se renderizan en el servidor o se generan como páginas estáticas, por el SEO.
  - Carrito, checkout y panel de administración corren en el navegador.
  - El carrito se guarda en el navegador (ADR 0014, punto 3).
- **Versiones fijas.** Se actualiza de a una versión mayor a la vez, con las herramientas de migración oficiales (codemods) y con las pruebas en verde.
- **Pruebas:**
  - Unitarias del carrito y de la selección de franja con Vitest y Testing Library. Vitest es de la familia de Vite.
  - El E2E sigue con Playwright y Lighthouse sigue igual.

## Consecuencias
- **A favor:**
  - Más salida laboral y mejor ayuda de la IA.
  - SSR y páginas estáticas maduros para el catálogo.
  - Se aprovecha lo que ya se sabe de React y Vite.
- **En contra:**
  - **Next.js ha tenido cambios fuertes entre versiones mayores** (el App Router, el comportamiento del caché, funciones que pasaron a ser asíncronas). Actualizar cuesta más que con `ng update` de Angular. Se mitiga con versiones fijas, actualizaciones de a una y pruebas.
  - **Lo hace una empresa (Vercel) que vende su propia nube**, y algunas funciones están pensadas primero para ella. Se mitiga desplegando con Docker propio y sin usar servicios exclusivos de Vercel.
  - **Hay que medir la memoria del servidor de Node** en local antes de elegir el tamaño del servidor, igual que con Angular SSR.
- **Enmienda al plan** (decisión 10, sección "Angular y SSR" y fase 5). El resto del plan no cambia.
