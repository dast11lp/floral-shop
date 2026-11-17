# Propuesta de diseño 1 (2026-11-16)

Copia de registro de la primera propuesta visual de la tienda. **No es código del proyecto** y no se usa para construir el frontend: es material de referencia.

## Estado
**Reemplazada por la propuesta 2** (`docs/diseno/propuesta-2`, ADR 0019). Se rehízo porque se buscaba un tono más juvenil y menos cuadrado, con otra tipografía y más color (pendiente 40 de `docs/pendientes.md`). Se guarda solo como historia.

## Qué hay
| Archivo | Pantalla |
|---|---|
| `Main.dc.html` | Celular: inicio |
| `M-Catalogo.dc.html` | Celular: catálogo |
| `M-Producto.dc.html` | Celular: producto |
| `M-Carrito.dc.html` | Celular: carrito |
| `M-Checkout.dc.html` | Celular: checkout (datos, zona, franja y pago) |
| `M-Resultado.dc.html` | Celular: pago aprobado |
| `D-Inicio.dc.html` | Computador: inicio |
| `D-Catalogo.dc.html` | Computador: catálogo |
| `D-Producto.dc.html` | Computador: producto |
| `Paletas.dc.html` | Cuatro paletas para comparar |
| `canvas.json` | Posición de cada pantalla en el lienzo |

## Cómo se ve
Son maquetas en HTML y CSS hechas para un lienzo de diseño. Cada archivo depende de un `support.js` que pone ese lienzo y que no está en este repositorio, así que **no se ven completas al abrirlas sueltas en el navegador**. El HTML y los estilos se pueden leer directamente en cada archivo.

## Qué es marcador
Las fotos (un dibujo de línea), el nombre "floristería", el teléfono, el correo, los productos y los precios son de ejemplo. No hay datos reales de la tienda (ADR 0002).

## Qué no copiar de aquí
- "Ocasiones" y "Categorías" se usan como si fueran lo mismo. Está por decidir si son dos clasificaciones (pendiente 39).
- No usa las herramientas del frontend real (ADR 0017): Next.js, Tailwind ni shadcn/ui.
