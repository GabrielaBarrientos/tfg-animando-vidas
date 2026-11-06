# Aplicación de la versión CORREGIDA del Incremento 2.11 — Encuestas

## Qué se corrigió

Este ZIP **reemplaza** `incremento-2.11-encuestas-taller.zip`. No hay que aplicar el ZIP anterior además de este.

1. `GET /proyecto`: `preguntas` pertenece a cada `clase`, **no** al taller. Se conservan todos los datos y clases anteriores.
2. Pregunta: cuatro atributos exactos — `idPreguntaPlataforma`, `enunciado`, `tipo` (`introducción`/`clase`) y `orden` consecutivo.
3. Identidades: mismo enunciado en distintas clases = IDs de pregunta distintos; mismo proyecto y número de clase = catálogo compartido por todos sus talleres.
4. `pregunta_proyecto`: sin `numero_clase`; columnas `id_pregunta_plataforma`, `enunciado`, `tipo`, `orden` y FK al proyecto, más la PK interna.
5. `GET /encuestas`: identificadores `idTallerPlataforma`, `idClasePlataforma`, `idPreguntaPlataforma`. Se validan antes de persistir.
6. La consulta de Pantalla 14 oculta **introducción**; conserva esas preguntas y respuestas en PostgreSQL para análisis con IA. Orden de presentación por `orden`.
7. Mock: 10 clases y 12 respuestas, de las cuales una es de introducción (se esperan 11 filas de respuestas visibles).

## Aplicación

Desde la raíz `tfg-animando-vidas` y con árbol limpio, ubicarse en `feat/encuestas-taller` o crearla desde `main` si todavía no existe:

```powershell
# Crear la rama solo si aún no existe:
git switch -c feat/encuestas-taller
# Colocar el ZIP en la carpeta padre del repositorio, o reemplazar la ruta:
Expand-Archive -Path ..\incremento-2.11-encuestas-taller-corregido.zip -DestinationPath . -Force
```

Si ya se había descomprimido el paquete anterior en la rama, se puede descomprimir **este paquete sobre los archivos existentes**, porque incluye todos los archivos del incremento más los tests actualizados. No mezclar con los ajustes pendientes de pantallas 8 y 12.

**Migración V10:** se corrigió el archivo de migración porque el incremento aún está en desarrollo. **Si la V10 anterior ya se ejecutó en PostgreSQL, no iniciés el backend con este paquete hasta preparar una migración de actualización:** Flyway detectaría el cambio de checksum. No borrar ni restablecer datos para resolverlo; informarlo para preparar un ajuste seguro.

## Datos simulados

- FastAPI lee talleres desde `api-simulada/app/talleres.json`, que **no se sobreescribe en el ZIP**.
- El JSON de ejemplo actualizado está en `api-simulada/ejemplo-talleres.json`.
- Si vas a probar con los datos de demostración y no necesitás preservar tu archivo local, respaldalo y copiá el ejemplo:

```powershell
Copy-Item api-simulada\app\talleres.json api-simulada\app\talleres.json.bak
Copy-Item api-simulada\ejemplo-talleres.json api-simulada\app\talleres.json -Force
```

- Si mantenés datos locales propios en `app/talleres.json`, todas sus clases deben tener el array `preguntas` con la nueva estructura; los datos de ejemplo no deben reemplazarse sobre ellos sin una copia previa.
- Para el ejemplo, todos los ID de preguntas y la clasificación de los enunciados son **ilustrativos**: validan la estructura, no afirman correspondencia con una API productiva.

## Pruebas locales

```powershell
cd api-simulada
python -m pytest -q

cd ..\backend
.\mvnw.cmd clean verify

cd ..\frontend
npm run lint
npm run build
```

El resultado esperado de las pruebas FastAPI del paquete corregido es **36 passed**. La migración SQL y las pruebas de Spring Boot y React necesitan validación local con Java 25, las dependencias instaladas y PostgreSQL.

## Prueba funcional

1. Iniciar FastAPI con los datos de ejemplo y el backend (si V10 no fue aplicada anteriormente).
2. En el proyecto, pulsar **Actualizar talleres** para incorporar el catálogo de preguntas de todas las clases.
3. Abrir el taller 8 → pestaña **Encuestas**, pulsar **ACTUALIZAR RESPUESTAS**.
4. Verificar que aparecen 11 respuestas de tipo clase en su orden correspondiente y que la pregunta de introducción no aparece, aunque está persistida.
5. Probar filtro por clase, doble actualización sin respuestas duplicadas y usuario externo con permisos de solo consulta.
6. Verificar `git status`, ejecutar las pruebas y recién entonces crear commit y Pull Request.

Detalles de modelo y contratos: `documentacion/incremento-2.11.md`.
