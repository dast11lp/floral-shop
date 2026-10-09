# 0005. Terraform como infraestructura como código

**Estado:** aceptada · **Fecha:** 2026-10-08

## Contexto
La infraestructura (EC2, RDS, S3, SQS, SES, ECR, IAM) debe ser reproducible y poder trasladarse a la cuenta de AWS de ella. Opciones: Terraform, CDK o la consola a mano.

## Decisión
Terraform, con módulos y variables genéricos y sin valores reales en el repositorio.
- Se valida primero contra Floci (fase 7) y se aplica después en AWS real (fase 8).
- El estado se guarda en un bucket de S3 remoto, con versionado y cifrado, porque puede contener datos sensibles. Ese bucket se crea antes que el resto.
- Los archivos `.tfvars` (donde van los valores reales: región, tamaño de la máquina, dominio) y el estado local están en `.gitignore`. Lo que se commitea es un `terraform.tfvars.example` con valores de ejemplo.

## Consecuencias
- **A favor:** funciona con muchos proveedores, así que sirve si algún día se cambia de nube o de VPS; es una herramienta muy usada; Floci lo soporta; y la infraestructura queda como código revisable.
- **En contra:** HCL es un lenguaje de configuración, menos cómodo que un lenguaje de programación. Y el bucket del estado es un problema de huevo y gallina que hay que resolver a mano la primera vez.
- **Nota:** existe OpenTofu, una bifurcación de código abierto compatible con Terraform, por si la licencia de Terraform llegara a importar.
- **Descartado:** CDK (ata a AWS) y la consola a mano (no es reproducible).
