
# Ejecución local del proyecto

## 1. Componentes

El prototipo de TFG está compuesto por:

- Frontend: React, TypeScript y Vite.
- Backend principal: Java y Spring Boot.
- API simulada: Python y FastAPI.
- Base de datos: PostgreSQL.

## 2. Requisitos

- Java 25
- Node.js 22 o superior compatible
- Python 3
- PostgreSQL
- Maven Wrapper (incluido en el backend)

Docker es opcional para ejecutar PostgreSQL.

## 3. Base de datos

Durante el desarrollo se utiliza PostgreSQL instalado localmente en Windows.

Configuración:

- Host: 127.0.0.1
- Puerto: 5432
- Base de datos: tfg_animando_vidas
- Usuario: tfg_app

La contraseña se proporciona mediante la variable de entorno DB_PASSWORD. No debe guardarse en Git.

## 4. Iniciar API simulada

Desde la carpeta api-simulada, con las dependencias instaladas en el entorno virtual:

    .\.venv\Scripts\python.exe -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000

Verificar:

http://127.0.0.1:8000/health

## 5. Iniciar backend

Desde la carpeta backend, configurar la contraseña de PostgreSQL en la terminal PowerShell:

    $securePassword = Read-Host "Contraseña de tfg_app" -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
    Remove-Variable securePassword

Iniciar:

    .\mvnw.cmd spring-boot:run

Verificar:

http://localhost:8080/api/health

El backend utiliza la propiedad educational-platform.base-url para comunicarse con FastAPI.

Por defecto apunta a http://127.0.0.1:8000.

## 6. Iniciar frontend

Desde la carpeta frontend, con las dependencias instaladas:

    npm run dev

Abrir:

http://localhost:5173

Vite utiliza un proxy para redirigir las solicitudes a /api hacia Spring Boot en el puerto 8080.

Verificar el proxy:

http://localhost:5173/api/health

## 7. Verificaciones automatizadas

Frontend:

    npm run lint
    npm run build

Backend:

    .\mvnw.cmd clean verify

Las pruebas del backend que requieren PostgreSQL necesitan que la base de datos esté disponible y
DB_PASSWORD esté configurada.

La prueba real con FastAPI está deshabilitada por defecto. Para ejecutarla:

    $env:RUN_PLATFORM_LIVE_TEST = "true"
    .\mvnw.cmd "-Dtest=EducationalPlatformLiveIntegrationTests" test
    Remove-Item Env:RUN_PLATFORM_LIVE_TEST

FastAPI debe estar iniciado para ejecutar esa prueba.

## 8. PostgreSQL mediante Docker (opcional)

Como alternativa a PostgreSQL instalado en Windows, se incluye una configuración Docker Compose
en infraestructura/compose.yaml.

El contenedor publica PostgreSQL en el puerto 127.0.0.1:5433 para evitar conflictos con la
instalación local.

Es necesario crear infraestructura/.env a partir de infraestructura/.env.example y configurar
las credenciales correspondientes.

Para iniciar el contenedor desde la raíz del proyecto:

    docker compose --env-file .\infraestructura\.env -f .\infraestructura\compose.yaml up -d

Si se utiliza esta alternativa, Spring Boot debe configurarse para conectarse al puerto 5433.

Las credenciales y los archivos .env no deben incorporarse al repositorio.

## 9. Consideraciones

- La API simulada utiliza una copia local de los datos de la plataforma educativa.
- La configuración actual corresponde al entorno de desarrollo, no a un despliegue productivo.
- No es necesario que todos los servicios estén iniciados para ejecutar las pruebas aisladas.
