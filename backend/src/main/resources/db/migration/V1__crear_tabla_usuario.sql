
CREATE TABLE usuario (
    id_usuario BIGINT GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,

    google_sub VARCHAR(255) UNIQUE,

    email VARCHAR(320) NOT NULL,
    nombres VARCHAR(200) NOT NULL,
    apellidos VARCHAR(200) NOT NULL,

    rol VARCHAR(20) NOT NULL,

    habilitado BOOLEAN NOT NULL DEFAULT FALSE,

    f_creacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    f_actualizacion TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_usuario_email_no_vacio
        CHECK (BTRIM(email) <> ''),

    CONSTRAINT ck_usuario_nombres_no_vacio
        CHECK (BTRIM(nombres) <> ''),

    CONSTRAINT ck_usuario_apellidos_no_vacio
        CHECK (BTRIM(apellidos) <> ''),

    CONSTRAINT ck_usuario_google_sub_no_vacio
        CHECK (
            google_sub IS NULL
            OR BTRIM(google_sub) <> ''
        ),

    CONSTRAINT ck_usuario_rol
        CHECK (
            rol IN (
                'ADMINISTRADOR',
                'OPERATIVO',
                'EXTERNO'
            )
        )
);

CREATE UNIQUE INDEX uq_usuario_email
    ON usuario (LOWER(BTRIM(email)));
