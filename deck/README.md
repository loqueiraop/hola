# Generador de la presentación

`Contrato de Franquicia - Grupo 1.pptx` se genera con estos dos archivos:

- `design.py` — sistema de diseño (paleta, tipografías, componentes reutilizables).
- `build.py` — contenido y composición de las 8 diapositivas.

## Regenerar el archivo

```bash
pip install python-pptx
cd deck && python3 build.py
```

El `.pptx` se escribe en la raíz del repositorio.

## Estructura

| # | Diapositiva |
|---|---|
| 1 | Portada |
| 2 | Componente 1 · Definición conceptual |
| 3 | Componente 2 · Cómo se aplica |
| 4 | Componente 3 · Partes involucradas |
| 5 | Componente 4 · Ventajas y desventajas |
| 6 | Componente 5 · Intro al video entrevista |
| 7 | Bibliografía |
| 8 | Cierre |

Cada diapositiva lleva notas del orador con la referencia de tiempo para
ajustarse a los 15 minutos.

## Ajustes frecuentes

- **Colores y tipografías**: constantes al inicio de `design.py`.
  Los títulos usan Georgia y el cuerpo Calibri; si se cambian, revisar que el
  texto siga cabiendo.
- **Texto**: cada diapositiva es un bloque comentado en `build.py`.
  Las cajas tienen medidas ajustadas, así que al alargar un texto conviene
  abrir el `.pptx` y comprobar que no se desborde de su tarjeta.
