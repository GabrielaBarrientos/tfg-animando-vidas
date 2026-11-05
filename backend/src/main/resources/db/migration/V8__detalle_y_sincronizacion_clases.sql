-- V8: información específica de clases informada por GET /taller.
-- No altera las migraciones anteriores ni las clases sincronizadas en V6-V7.
ALTER TABLE clase_taller
    ADD COLUMN nombre_clase VARCHAR(500),
    ADD COLUMN nombre_modulo VARCHAR(500),
    ADD COLUMN estado_clase VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';

UPDATE clase_taller SET estado_clase = 'REALIZADA' WHERE fecha_realizacion IS NOT NULL;

ALTER TABLE clase_taller
    ADD CONSTRAINT ck_clase_taller_estado
    CHECK (estado_clase IN ('REALIZADA', 'PENDIENTE') AND
           ((estado_clase = 'REALIZADA' AND fecha_realizacion IS NOT NULL) OR
            (estado_clase = 'PENDIENTE' AND fecha_realizacion IS NULL)));

ALTER TABLE taller
    ADD COLUMN ultima_sincronizacion_clases TIMESTAMPTZ;
