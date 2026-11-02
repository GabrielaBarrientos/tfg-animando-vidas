-- V4: parámetros de seguimiento y destinatarios de informes.
-- No se programan ni envían correos desde este prototipo.
CREATE TABLE configuracion_seguimiento (
    id_configuracion_seguimiento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proyecto BIGINT NOT NULL UNIQUE,
    umbral_proximo_atraso_semanas INTEGER NOT NULL,
    umbral_atraso_semanas INTEGER NOT NULL,
    id_responsable_seguimiento BIGINT NOT NULL,
    dia_informe_semanal VARCHAR(12) NOT NULL,
    id_responsable_proyecto BIGINT NOT NULL,
    tipo_dia_informe_mensual VARCHAR(20) NOT NULL,
    dia_informe_mensual INTEGER,
    f_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    f_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_config_seguimiento_proyecto FOREIGN KEY (id_proyecto)
        REFERENCES proyecto(id_proyecto) ON DELETE RESTRICT,
    CONSTRAINT fk_config_seguimiento_responsable_semanal FOREIGN KEY (id_responsable_seguimiento)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_config_seguimiento_responsable_mensual FOREIGN KEY (id_responsable_proyecto)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_config_seguimiento_umbrales CHECK (
        umbral_proximo_atraso_semanas > 0
        AND umbral_atraso_semanas > umbral_proximo_atraso_semanas
    ),
    CONSTRAINT ck_config_seguimiento_dia_semanal CHECK (
        dia_informe_semanal IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')
    ),
    CONSTRAINT ck_config_seguimiento_dia_mensual CHECK (
        (tipo_dia_informe_mensual = 'ULTIMO_DIA_HABIL' AND dia_informe_mensual IS NULL)
        OR (tipo_dia_informe_mensual = 'DIA_DEL_MES' AND dia_informe_mensual IS NOT NULL
            AND dia_informe_mensual BETWEEN 1 AND 31)
    )
);

CREATE INDEX idx_config_seguimiento_responsable_semanal
    ON configuracion_seguimiento (id_responsable_seguimiento);
CREATE INDEX idx_config_seguimiento_responsable_mensual
    ON configuracion_seguimiento (id_responsable_proyecto);
