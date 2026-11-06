
# Sistema de gestión y seguimiento de talleres educativos de Animando Vidas

## Descripción

Prototipo de aplicación web para el seguimiento y gestión de talleres educativos de la ONG Animando Vidas.

El sistema permite realizar el seguimiento del avance de los talleres, controlar evidencias, registrar comunicaciones con docentes y generar información útil para la gestión de los proyectos.

Además, incorpora el análisis cualitativo de respuestas abiertas de encuestas mediante un modelo de lenguaje, con validación humana de los resultados.

## Tecnologías

- **Frontend:** React, TypeScript y Vite.
- **Backend:** Java 25 LTS y Spring Boot.
- **API simulada:** Python y FastAPI.
- **Base de datos:** PostgreSQL.
- **Autenticación:** Google Identity.
- **Análisis cualitativo:** API de OpenAI.
- **Almacenamiento de evidencias:** Google Drive.
- **Contenedores:** Docker y Docker Compose.

## Estructura del proyecto

- `frontend/`: aplicación web desarrollada con React.
- `backend/`: API REST y lógica de negocio con Spring Boot.
- `api-simulada/`: API de consulta a los datos de la plataforma educativa.
- `infraestructura/`: configuraciones de contenedores y despliegue.
- `documentacion/`: documentación técnica y pruebas.

## Metodología de desarrollo

El proyecto utiliza un enfoque de desarrollo iterativo incremental.

Cada funcionalidad comprende análisis, diseño técnico, implementación, integración y pruebas antes de considerarse terminada.

## Estado del proyecto

En desarrollo.

Se encuentran implementadas las funcionalidades de gestión de proyectos, planificación, configuración de seguimiento,
gestión de docentes, sincronización de talleres, consulta de clases, evidencias, encuestas y gestión de comunicaciones con docentes.

Las funcionalidades restantes se desarrollarán en los próximos incrementos.