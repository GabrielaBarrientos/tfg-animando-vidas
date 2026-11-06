-- Incremento 2.12: incorporar Consulta a Acción realizada.
-- No modificar V13: puede encontrarse ejecutada en bases de pruebas.
-- Conserva todas las comunicaciones existentes.
ALTER TABLE comunicacion_taller
    DROP CONSTRAINT ck_comunicacion_accion;

ALTER TABLE comunicacion_taller
    ADD CONSTRAINT ck_comunicacion_accion
    CHECK (accion_realizada IN ('RECORDATORIO', 'RECLAMO', 'SEGUIMIENTO', 'CONSULTA'));
