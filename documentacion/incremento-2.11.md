# Incremento 2.11 — Pantalla 14: Encuestas del taller (contrato corregido)

## Objetivo

Consultar desde PostgreSQL las preguntas y respuestas de las clases de un taller, sincronizadas manualmente con la plataforma educativa, conservando los registros de introducción para un futuro análisis con IA.

## Contrato `GET /proyecto`

- La respuesta sigue siendo un array de talleres, con todos sus atributos originales y **todas sus clases** (realizadas y pendientes).
- **Cada clase** tiene un array `preguntas`; **no** existe un catálogo `preguntas` a nivel de taller.
- Cada pregunta contiene únicamente `idPreguntaPlataforma` (entero numérico incremental), `enunciado` (texto), `tipo` (`introducción` o `clase`) y `orden` (1, 2, 3… dentro de cada clase).
- Los mismos enunciados en distintas clases tienen **distintos** `idPreguntaPlataforma`.
- Para un mismo proyecto, todos los talleres tienen las mismas preguntas por `numeroClase`, con los mismos identificadores, tipos, enunciados y órdenes, aunque cada taller tenga sus propios `idClasePlataforma`.
- El backend valida identificadores únicos dentro de cada taller, órdenes sin saltos y coherencia de los catálogos entre talleres **antes** de persistir el lote.

Fragmento del contrato:

```json
{
  "idTaller": "8",
  "clases": [
    {
      "idClasePlataforma": 7,
      "numeroClase": 1,
      "preguntas": [
        {"idPreguntaPlataforma": 101, "enunciado": "¿Te resultó fácil adaptarte a la plataforma?", "tipo": "introducción", "orden": 1},
        {"idPreguntaPlataforma": 102, "enunciado": "Para establecer un diagnóstico...", "tipo": "clase", "orden": 2}
      ]
    }
  ]
}
```

Los ejemplos de ID y de clasificación de enunciados del JSON simulado son **datos de prueba**, no los IDs reales de la plataforma educativa.

## Contrato `GET /encuestas`

`GET /encuestas?id_taller_plataforma=8` responde:

```json
{
  "idTallerPlataforma": 8,
  "respuestas": [
    {
      "idTallerPlataforma": 8,
      "idClasePlataforma": 7,
      "idPreguntaPlataforma": 102,
      "respuesta": "Todos comprendieron observando el video."
    }
  ]
}
```

Se valida que cada respuesta corresponda al taller consultado y que el ID externo de la clase y el de la pregunta existan localmente en ese proyecto. No se usa el texto del enunciado para buscar ni relacionar respuestas. Las respuestas se almacenan por `encuesta_taller` + `pregunta_proyecto`.

## Persistencia — migración Flyway V10

- `pregunta_proyecto`: `id_pregunta_proyecto` (PK artificial), `id_proyecto` (FK), `id_pregunta_plataforma` (UNIQUE), `enunciado`, `tipo` (internamente `INTRODUCCION` o `CLASE`) y `orden`. **No contiene `numero_clase`.**
- `encuesta_taller`: una encuesta por `id_clase_taller` (FK única) y fechas de primera/última sincronización.
- `respuesta_encuesta`: `id_encuesta_taller` (FK), `id_pregunta_proyecto` (FK), valor y fechas de sincronización. Se impide repetir una respuesta para la misma encuesta y pregunta.
- `taller.ultima_sincronizacion_encuestas`: fecha de última sincronización exitosa de respuestas.
- El catálogo se persiste una sola vez por proyecto; las preguntas no se duplican por taller. Los IDs externos se usan exclusivamente para buscar los registros correctos.

**Importante:** la migración V10 incluida es la versión corregida del paquete no aplicado. Si la V10 anterior ya fue ejecutada en una base de datos, no debe sobrescribirse ni volver a aplicarse sin una migración adicional planificada, para no alterar el historial Flyway ni los datos.

## Pantalla 14

- Al entrar en Encuestas se lee la base local, sin sincronizar automáticamente.
- Se muestran las respuestas de preguntas con `tipo=clase`, agrupadas por clase y ordenadas por `orden`.
- Las preguntas de `tipo=introducción` y sus respuestas permanecen en PostgreSQL para el futuro análisis con IA, pero **no se muestran** en esta pantalla ni en la respuesta del endpoint de consulta de la pantalla.
- El filtro de clase afecta solo la presentación; no modifica el alcance del análisis con IA.
- Administrador/Operativo autorizado pueden actualizar respuestas; usuario Externo solo consulta.
- `ANALIZAR CON IA` continúa deshabilitado hasta los incrementos de configuración, ejecución y validación humana.
- La pantalla refleja las respuestas recuperadas; si una pregunta aún no tiene respuesta, no aparece como fila contestada. No hay un vínculo `numero_clase` en `pregunta_proyecto` ni se inventa uno.

## Datos y pruebas

`api-simulada/ejemplo-talleres.json` incluye 10 clases y sus arrays de preguntas (también la clase pendiente); los ID de preguntas no se reutilizan entre distintas clases. `api-simulada/app/encuestas.json` contiene 12 respuestas simuladas, incluida una de introducción. La Pantalla 14 mostrará las otras 11 respuestas.

Pruebas FastAPI: contrato por clase; catálogo compartido entre talleres; identificadores distintos para el mismo enunciado; validación de tipos, orden, duplicados; respuesta con los tres ID externos. Pruebas Java: sincronización y filtrado de introducciones.
