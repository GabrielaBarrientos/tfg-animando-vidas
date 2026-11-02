# Incremento 2.5 — Configuración del seguimiento y notificaciones

## Alcance implementado

Se incorporan la Pantalla 5, la persistencia de parámetros de seguimiento y el
cambio de estado de proyecto al guardar correctamente la configuración.

No se generan ni se envían correos electrónicos, no se ejecutan tareas programadas
y no se almacena un historial de envíos. Los días configurados representan
**preferencias para informes futuros**, no un calendario de ejecuciones en marcha.

## Datos persistidos

Tabla `configuracion_seguimiento`, creada por Flyway `V4__crear_tabla_configuracion_seguimiento.sql`:

- Proyecto (uno a uno, obligatorio).
- Umbral de próximo atraso (semanas; inicial en UI: 2).
- Umbral de atraso (semanas; inicial en UI: 3, siempre mayor al anterior).
- Responsable de seguimiento (usuario interno; destinatario del informe semanal).
- Día de informe semanal (lunes a domingo).
- Responsable del proyecto (usuario interno; destinatario del informe mensual).
- Modalidad de informe mensual: `ULTIMO_DIA_HABIL` o `DIA_DEL_MES` (1 a 31).
- Timestamps de creación y actualización.

La configuración usa el usuario existente y **no altera** `usuario_proyecto`.
Para usuarios `OPERATIVO`, la elegibilidad exige asignación previa al proyecto.
Los `ADMINISTRADOR` habilitados son elegibles para todos los proyectos; los
`EXTERNO` y usuarios deshabilitados no son elegibles.

El valor `ULTIMO_DIA_HABIL` expresa último día de lunes a viernes, sin feriados.
No se calcula ni dispara una fecha de envío en este incremento. Para días fijos
29, 30 o 31, el comportamiento en meses más cortos deberá decidirse al agregar
la funcionalidad de envío, que está fuera del alcance de este prototipo.

## Reglas y permisos

1. Solo `ADMINISTRADOR` puede consultar o guardar la configuración y listar responsables elegibles.
2. Primero debe existir un proyecto con planificación guardada (Pantalla 4).
3. Los umbrales deben ser enteros positivos y `próximo atraso < atraso`.
4. Los destinatarios deben ser usuarios internos habilitados; el operativo debe estar previamente asignado.
5. `DIA_DEL_MES` requiere un entero entre 1 y 31; `ULTIMO_DIA_HABIL` exige `null`.
6. Guardar una configuración nueva o actualizar la existente se hace con `PUT`.
7. Guardar exitosamente cambia el proyecto de `PENDIENTE_PLANIFICACION` a `PLANIFICADO` en la misma transacción. Si el proyecto ya está `PLANIFICADO`, `EN_CURSO` o `FINALIZADO`, no se retrocede su estado.
8. En caso de error de validación, no se guarda ni cambia el estado del proyecto.
9. La configuración no concede permisos de lectura ni modifica asignaciones de proyectos.
10. El prototipo no envía notificaciones.

## Endpoints HTTP

Todos exigen sesión Google válida, permisos de administrador y CSRF en PUT:

| Método | URL | Respuesta |
|---|---|---|
| GET | `/api/proyectos/{idProyecto}/responsables-elegibles` | Lista de usuarios elegibles |
| GET | `/api/proyectos/{idProyecto}/seguimiento` | Configuración o 404 |
| PUT | `/api/proyectos/{idProyecto}/seguimiento` | Crea/actualiza, o 400/403/404 |

Ejemplo del cuerpo del PUT:

```json
{
  "umbralProximoAtrasoSemanas": 2,
  "umbralAtrasoSemanas": 3,
  "idResponsableSeguimiento": 1,
  "diaInformeSemanal": "VIERNES",
  "idResponsableProyecto": 1,
  "tipoDiaInformeMensual": "ULTIMO_DIA_HABIL",
  "diaInformeMensual": null
}
```

## Flujo de frontend

- Pantalla 2: un administrador puede abrir la configuración de proyectos pendientes o planificados.
- Pantalla 4: aparece **Continuar a seguimiento** únicamente cuando la planificación está guardada y no hay cambios pendientes.
- Pantalla 5: carga destinatarios elegibles y configuración preexistente; permite guardar, reintentar consultas, volver a planificación o volver al listado.
- Después del guardado se muestra confirmación y el estado planificado; la lista se actualiza al regresar.
- La Pantalla 6 — Gestión de docentes no se implementa en este incremento.

## Verificación en Windows

Desde la rama `feat/configuracion-seguimiento`, con PostgreSQL configurado y
el entorno local funcionando:

```powershell
cd backend
.\mvnw.cmd verify
cd ..\frontend
npm ci
npm run lint
npm run build
```

Después, iniciar los servicios según `documentacion/ejecucion-local.md` y probar:

1. Crear un proyecto y guardar planificación.
2. Entrar en Seguimiento. Probar los valores iniciales 2 y 3.
3. Seleccionar responsables elegibles y modalidades de envío. Guardar y verificar «Planificado».
4. Regresar y volver a abrir la configuración; comprobar que se conserva.
5. Actualizarla y verificar que no se duplicó el registro.
6. Probar umbrales inválidos, días fuera del rango, usuario no elegible, ausencia de CSRF y sesión expirada.
7. Comprobar que no se envía ningún correo.

Los tests de servicio y controller están bajo `backend/src/test/java/org/tfg/backend/seguimiento/`.
