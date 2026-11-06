# Corrección Incremento 2.11 — Toast de sincronización de respuestas

**Alcance:** exclusivamente `frontend/src/components/EncuestasTaller.tsx`.

- Reemplaza el aviso inline de éxito (por ejemplo, «12 respuestas procesadas correctamente.») por un `Snackbar` de Material UI.
- Flota en la esquina superior derecha, respetando los estilos usados en los toasts de Planificación/Seguimiento: `Alert` de éxito `filled`, fondo `colores.verdeToastExito`, texto blanco, ancho responsivo y sombra.
- Desaparece automáticamente a los 5 segundos o al cerrar manualmente.
- Conserva el botón LIMPIAR FILTRO, la tabla, la sincronización y el aviso de error existente.

## Aplicación

Desde la raíz del repositorio, en la rama `feat/encuestas-taller`:

```powershell
Expand-Archive -Path ..\correccion-2.11-toast-respuestas.zip -DestinationPath . -Force
cd frontend
npm run lint
npm run build
```

El ZIP sobrescribe un solo archivo de código y no modifica base de datos ni backend.
