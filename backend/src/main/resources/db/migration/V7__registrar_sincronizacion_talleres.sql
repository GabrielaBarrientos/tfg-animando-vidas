-- V7: estado y metadatos de la sincronización de talleres por proyecto.
-- Se mantienen intactas las migraciones V1-V6 y las copias de talleres existentes.
ALTER TABLE proyecto
    ADD COLUMN ultima_sincronizacion_talleres TIMESTAMPTZ,
    ADD COLUMN cantidad_talleres_plataforma INTEGER NOT NULL DEFAULT 0;

ALTER TABLE proyecto
    ADD CONSTRAINT ck_proyecto_cantidad_talleres_plataforma
    CHECK (cantidad_talleres_plataforma >= 0);

-- Recuperar información aproximada de las sincronizaciones anteriores a V7
-- a partir de las copias locales disponibles (no existía contador por proyecto).
UPDATE proyecto p
SET ultima_sincronizacion_talleres = (
        SELECT MAX(t.fecha_sincronizacion) FROM taller t WHERE t.id_proyecto = p.id_proyecto
    ),
    cantidad_talleres_plataforma = (
        SELECT COUNT(*) FROM taller t
        WHERE t.id_proyecto = p.id_proyecto AND NOT t.pendiente_baja
    )
WHERE EXISTS (SELECT 1 FROM taller t WHERE t.id_proyecto = p.id_proyecto);

-- Los talleres ya cargados mediante V6 deben reflejarse en el estado del proyecto.
UPDATE proyecto p
SET estado = 'EN_CURSO'
WHERE p.estado = 'PLANIFICADO'
  AND EXISTS (SELECT 1 FROM taller t WHERE t.id_proyecto = p.id_proyecto);

UPDATE proyecto p
SET estado = 'PLANIFICADO'
WHERE p.estado = 'EN_CURSO'
  AND NOT EXISTS (SELECT 1 FROM taller t WHERE t.id_proyecto = p.id_proyecto);
