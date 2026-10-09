# 0006. Secretos y configuración

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
La aplicación necesita configuración distinta por entorno (local, pruebas, producción) y secretos reales (llaves de la pasarela, contraseña de la base de datos). Nada de eso puede estar en el repositorio.

## Decisión
- **Configuración:** perfiles de Spring (`local`, `test`, `prod`) más variables de entorno. La configuración `local` se commitea porque solo tiene valores de ejemplo.
- **Local:** Docker Compose lee un `.env` (ignorado por Git, con valores inventados). Un `.env.example` se commitea como plantilla.
- **Producción:** los secretos de la aplicación viven en **SSM Parameter Store**, como parámetros `SecureString` de nivel estándar, en la cuenta de ella.
- **GitHub Actions:** se autentica en AWS con OIDC, sin llaves permanentes.
- **Credenciales de AWS en la app:** cadena por defecto del SDK (valores falsos en local, rol IAM en la EC2).
- No se usa un `.env` en la EC2.

## Consecuencias
- **A favor:** ningún secreto en el código ni en el historial, un solo mecanismo de configuración en todos los entornos y, según los precios consultados, el nivel estándar de SSM Parameter Store no tiene costo de almacenamiento.
- **En contra:** más piezas de AWS que configurar (roles, políticas, proveedor OIDC). SSM no rota los secretos automáticamente.
- **Cuándo se revisa:** si algún día se necesita rotación automática de credenciales, se mueve ese secreto a Secrets Manager, que cobra por cada secreto al mes (alrededor de US$0,40 según lo consultado; verificar el precio vigente).
- **Descartado:** `.env` en la EC2 (sin control de acceso ni rotación), un archivo por entorno con datos reales y un servidor de configuración (exagerado para este tamaño).
