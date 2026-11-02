-- V5: docentes por proyecto y registro de asociaciones conocidas con talleres.
CREATE TABLE docente (
    id_docente BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proyecto BIGINT NOT NULL REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT,
    nombres VARCHAR(200) NOT NULL,
    apellidos VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    pais VARCHAR(100) NOT NULL,
    provincia VARCHAR(150) NOT NULL,
    centro_educativo VARCHAR(200) NOT NULL,
    f_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    f_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_docente_campos_requeridos CHECK (
        BTRIM(nombres) <> '' AND BTRIM(apellidos) <> '' AND BTRIM(email) <> ''
        AND BTRIM(pais) <> '' AND BTRIM(provincia) <> '' AND BTRIM(centro_educativo) <> ''
    )
);
CREATE INDEX idx_docente_proyecto ON docente(id_proyecto);
CREATE UNIQUE INDEX uq_docente_email_por_proyecto
    ON docente (id_proyecto, LOWER(BTRIM(email)));

-- La sincronización futura deberá registrar las asociaciones de la plataforma.
-- La comprobación de eliminación cubre las asociaciones almacenadas aquí,
-- NO consulta talleres remotos que aún no se sincronizaron.
CREATE TABLE asociacion_taller_docente (
    id_taller_plataforma VARCHAR(150) PRIMARY KEY,
    id_docente BIGINT NOT NULL,
    CONSTRAINT fk_asociacion_taller_docente FOREIGN KEY (id_docente)
        REFERENCES docente(id_docente) ON DELETE RESTRICT
);
CREATE INDEX idx_asociacion_taller_docente ON asociacion_taller_docente(id_docente);
