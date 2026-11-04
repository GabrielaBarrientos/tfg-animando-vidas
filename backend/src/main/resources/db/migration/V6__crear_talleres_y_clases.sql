-- V6: docentes compartidos entre proyectos, datos de participación y talleres.
-- Esta migración reorganiza las tablas de V5. La base de desarrollo confirmada no tiene docentes.
-- Si ya se utilizaron estas tablas, abortar antes de perder asociaciones existentes.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM docente)
       OR EXISTS (SELECT 1 FROM asociacion_taller_docente) THEN
        RAISE EXCEPTION 'V6 requiere migrar previamente los docentes existentes; no se eliminarán datos';
    END IF;
END $$;

DROP TABLE asociacion_taller_docente;
DROP INDEX uq_docente_email_por_proyecto;
DROP INDEX idx_docente_proyecto;
ALTER TABLE docente DROP CONSTRAINT ck_docente_campos_requeridos;
ALTER TABLE docente DROP COLUMN id_proyecto;
ALTER TABLE docente DROP COLUMN pais;
ALTER TABLE docente DROP COLUMN provincia;
ALTER TABLE docente DROP COLUMN centro_educativo;
ALTER TABLE docente ADD COLUMN telefono VARCHAR(30) NOT NULL;
ALTER TABLE docente ADD CONSTRAINT ck_docente_personales CHECK (
    BTRIM(nombres) <> '' AND BTRIM(apellidos) <> '' AND BTRIM(email) <> ''
    AND BTRIM(telefono) <> ''
);
-- La coincidencia de correo identifica un mismo registro personal en distintos proyectos.
CREATE UNIQUE INDEX uq_docente_email ON docente (LOWER(BTRIM(email)));

CREATE TABLE docente_proyecto (
    id_docente BIGINT NOT NULL REFERENCES docente(id_docente) ON DELETE RESTRICT,
    id_proyecto BIGINT NOT NULL REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT,
    usuario_plataforma VARCHAR(150) NOT NULL,
    pais VARCHAR(100) NOT NULL,
    provincia VARCHAR(150) NOT NULL,
    centro_educativo VARCHAR(200) NOT NULL,
    cargo VARCHAR(150) NOT NULL,
    grado VARCHAR(100) NOT NULL,
    materia VARCHAR(150) NOT NULL,
    carga_horaria INTEGER NOT NULL,
    cantidad_alumnos INTEGER NOT NULL,
    f_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    f_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_docente_proyecto PRIMARY KEY (id_docente, id_proyecto),
    CONSTRAINT ck_docente_proyecto_requeridos CHECK (
        BTRIM(pais) <> '' AND BTRIM(provincia) <> '' AND BTRIM(centro_educativo) <> ''
        AND BTRIM(cargo) <> '' AND BTRIM(grado) <> '' AND BTRIM(materia) <> ''
        AND BTRIM(usuario_plataforma) <> ''
    ),
    CONSTRAINT ck_docente_proyecto_enteros CHECK (carga_horaria > 0 AND cantidad_alumnos > 0),
    CONSTRAINT ck_docente_proyecto_usuario_formato CHECK (
        LENGTH(usuario_plataforma) BETWEEN 8 AND 150
        AND usuario_plataforma ~ '^[a-z]+(\.[a-z]+)+$'
    )
);
CREATE INDEX idx_docente_proyecto_proyecto ON docente_proyecto(id_proyecto);
CREATE UNIQUE INDEX uq_docente_proyecto_usuario ON docente_proyecto
    (id_proyecto, LOWER(BTRIM(usuario_plataforma)));

CREATE TABLE taller (
    id_taller_plataforma VARCHAR(150) PRIMARY KEY,
    id_proyecto BIGINT NOT NULL,
    id_docente BIGINT NOT NULL,
    fecha_sincronizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pendiente_baja BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_taller_docente_proyecto FOREIGN KEY (id_docente, id_proyecto)
        REFERENCES docente_proyecto (id_docente, id_proyecto) ON DELETE RESTRICT,
    CONSTRAINT uq_taller_docente_proyecto UNIQUE (id_proyecto, id_docente)
);
CREATE INDEX idx_taller_proyecto ON taller (id_proyecto);

CREATE TABLE clase_taller (
    id_taller_plataforma VARCHAR(150) NOT NULL REFERENCES taller(id_taller_plataforma) ON DELETE CASCADE,
    numero_clase INTEGER NOT NULL CHECK (numero_clase > 0),
    fecha_realizacion DATE,
    cantidad_participantes INTEGER CHECK (cantidad_participantes IS NULL OR cantidad_participantes >= 0),
    cantidad_evidencias INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_evidencias >= 0),
    CONSTRAINT pk_clase_taller PRIMARY KEY (id_taller_plataforma, numero_clase),
    CONSTRAINT ck_clase_taller_datos CHECK (
        (fecha_realizacion IS NULL AND cantidad_participantes IS NULL AND cantidad_evidencias = 0)
        OR (fecha_realizacion IS NOT NULL AND cantidad_participantes IS NOT NULL)
    )
);
