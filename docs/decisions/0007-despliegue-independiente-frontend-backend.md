# 0007. Despliegue independiente de frontend y backend

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
Frontend y backend viven en el mismo repositorio, pero un cambio en uno no debería obligar a reiniciar el otro.

## Decisión
- Dos imágenes de Docker y dos workflows de despliegue, con filtros de rutas: `backend/**` y `frontend/**`. Cada uno construye su imagen, la sube a ECR y reinicia solo su contenedor.
- En cada pull request corre un único workflow `ci` (las pruebas automáticas). Su primer job detecta qué carpetas cambiaron para correr solo lo necesario, y un job final `ci-ok` resume el resultado de todos. `ci-ok` es el único check obligatorio para mergear a `main`. Se hace así porque, si se exigiera un check de una carpeta que no cambió, ese check nunca correría y el PR quedaría bloqueado esperándolo.
- Desde la fase 5, cuando ya existe el frontend, corre también una prueba E2E (extremo a extremo): un robot con navegador que hace lo que haría un cliente, de elegir un arreglo a pagar en sandbox. Corre contra todo el sistema en cada PR, aunque solo haya cambiado una parte, porque los errores suelen aparecer en la unión entre frontend y backend.
- La API está versionada (`/api/v1`) y descrita con OpenAPI.
- Los cambios son compatibles hacia atrás: primero se despliega el backend que acepta lo viejo y lo nuevo, luego el frontend, y al final se retira lo viejo.
- Las migraciones de base de datos corren con el arranque del backend (Flyway).

## Consecuencias
- **A favor:** despliegues más pequeños y rápidos, y un fallo en uno no tumba el otro.
- **En contra:** pueden quedar desalineados durante un momento; se mitiga con las reglas de compatibilidad, el E2E y las pruebas de contrato.
- **Alcance:** cada despliegue implica un reinicio breve del servicio que cambió. Cero downtime exigiría un balanceador y dos instancias, y no hace falta hoy.
