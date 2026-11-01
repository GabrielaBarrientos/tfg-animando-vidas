
-- V3: Planificación de proyectos

CREATE TABLE planificacion_proyecto (
    id_planificacion_proyecto BIGINT GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,

    id_proyecto BIGINT NOT NULL,

    fecha_inicio DATE NOT NULL,

    fecha_finalizacion DATE NOT NULL,

    cantidad_clases INTEGER NOT NULL,

    frecuencia_clases VARCHAR(20) NOT NULL,

    f_creacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    f_actualizacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_planificacion_proyecto_proyecto
        UNIQUE (id_proyecto),

    CONSTRAINT fk_planificacion_proyecto_proyecto
        FOREIGN KEY (id_proyecto)
        REFERENCES proyecto(id_proyecto)
        ON DELETE RESTRICT,

    CONSTRAINT ck_planificacion_proyecto_fechas
        CHECK (fecha_finalizacion > fecha_inicio),

    CONSTRAINT ck_planificacion_proyecto_cantidad_clases
        CHECK (cantidad_clases > 0),

    CONSTRAINT ck_planificacion_proyecto_frecuencia
        CHECK (
            frecuencia_clases IN (
                'SEMANAL',
                'QUINCENAL',
                'MENSUAL'
            )
        )
);
