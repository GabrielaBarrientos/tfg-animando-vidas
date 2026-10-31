
-- V2: Empresas financiadoras, proyectos y asignaciones

-- 1. Empresas financiadoras

CREATE TABLE empresa (
    id_empresa BIGINT GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,

    nombre VARCHAR(200) NOT NULL,

    f_creacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    f_actualizacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_empresa_nombre_no_vacio
        CHECK (BTRIM(nombre) <> '')
);

CREATE UNIQUE INDEX uq_empresa_nombre
    ON empresa (LOWER(BTRIM(nombre)));


-- 2. Relacionar usuarios con empresas

ALTER TABLE usuario
    ADD COLUMN id_empresa BIGINT;

ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_empresa
        FOREIGN KEY (id_empresa)
        REFERENCES empresa(id_empresa)
        ON DELETE RESTRICT;

CREATE INDEX idx_usuario_empresa
    ON usuario (id_empresa);


-- 3. Proyectos

CREATE TABLE proyecto (
    id_proyecto BIGINT GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,

    nombre VARCHAR(200) NOT NULL,

    anio SMALLINT NOT NULL,

    descripcion TEXT,

    id_empresa BIGINT NOT NULL,

    estado VARCHAR(50) NOT NULL
        DEFAULT 'PENDIENTE_PLANIFICACION',

    f_creacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    f_actualizacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_proyecto_nombre_no_vacio
        CHECK (BTRIM(nombre) <> ''),

    CONSTRAINT ck_proyecto_anio
        CHECK (anio > 0),

    CONSTRAINT ck_proyecto_estado
        CHECK (
            estado IN (
                'PENDIENTE_PLANIFICACION',
                'PLANIFICADO',
                'EN_CURSO',
                'FINALIZADO'
            )
        ),

    CONSTRAINT fk_proyecto_empresa
        FOREIGN KEY (id_empresa)
        REFERENCES empresa(id_empresa)
        ON DELETE RESTRICT
);

CREATE INDEX idx_proyecto_empresa
    ON proyecto (id_empresa);

CREATE INDEX idx_proyecto_estado
    ON proyecto (estado);


-- 4. Asignaciones de usuarios a proyectos

CREATE TABLE usuario_proyecto (
    id_usuario BIGINT NOT NULL,
    id_proyecto BIGINT NOT NULL,

    CONSTRAINT pk_usuario_proyecto
        PRIMARY KEY (id_usuario, id_proyecto),

    CONSTRAINT fk_usuario_proyecto_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON DELETE RESTRICT,

    CONSTRAINT fk_usuario_proyecto_proyecto
        FOREIGN KEY (id_proyecto)
        REFERENCES proyecto(id_proyecto)
        ON DELETE RESTRICT
);

CREATE INDEX idx_usuario_proyecto_proyecto
    ON usuario_proyecto (id_proyecto);
