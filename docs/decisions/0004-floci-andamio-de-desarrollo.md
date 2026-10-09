# 0004. Floci como andamio de desarrollo, no como dependencia

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
Se quiere aprender y desarrollar contra servicios de AWS sin depender de una cuenta real ni gastar créditos. Floci es un emulador de AWS gratuito y de código abierto, que corre en un contenedor y escucha en `localhost:4566`.

## Decisión
Floci se usa solo en desarrollo y en pruebas (S3, SQS, SES, secretos y, si hace falta, la validación de Terraform):
- Corre en `docker-compose.yml` y en las pruebas de integración con Testcontainers.
- Nunca se despliega. En producción lo reemplazan los servicios reales de AWS.
- Su endpoint solo existe en el perfil `local`. Las credenciales se resuelven por la cadena por defecto del SDK: valores falsos en local y rol IAM en AWS.
- Se fija una versión concreta de la imagen, no `latest`.
- Antes de dar algo por bueno, se hace una prueba de humo contra AWS real.

## Consecuencias
- **A favor:** desarrollo sin cuenta de AWS ni costos, pruebas reproducibles y un camino directo a producción cambiando solo la configuración.
- **En contra:** la paridad con AWS no es total. El soporte de EC2 e IAM puede ser parcial, y puede haber diferencias de comportamiento (por ejemplo, SES o el acceso a S3). Eso se compensa con `terraform plan` y con la prueba de humo en AWS real.
- **Mitigación:** el código habla con puertos (`FileStorage`, `EmailSender`), así que una diferencia se arregla dentro de un adaptador. Si Floci deja de servir, se cambia de emulador o se prueba contra AWS real.
