-- V12: renombrar las tablas de configuracion y evidencias.
-- Los registros, las claves, las relaciones y los contratos HTTP no cambian.
-- PostgreSQL ejecuta la migracion mediante Flyway en una transaccion.
-- No modificar V1-V11 ni borrar el historial de Flyway.

-- Tablas (se conservan sus columnas y los datos existentes).
ALTER TABLE configuracion_seguimiento RENAME TO configuracion_proyecto;
ALTER TABLE evidencia RENAME TO evidencia_taller;

-- Restricciones de configuracion_proyecto (creadas en V4).
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT configuracion_seguimiento_pkey TO configuracion_proyecto_pkey;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT configuracion_seguimiento_id_proyecto_key TO configuracion_proyecto_id_proyecto_key;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT fk_config_seguimiento_proyecto TO fk_config_proyecto_proyecto;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT fk_config_seguimiento_responsable_semanal TO fk_config_proyecto_responsable_semanal;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT fk_config_seguimiento_responsable_mensual TO fk_config_proyecto_responsable_mensual;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT ck_config_seguimiento_umbrales TO ck_config_proyecto_umbrales;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT ck_config_seguimiento_dia_semanal TO ck_config_proyecto_dia_semanal;
ALTER TABLE configuracion_proyecto
    RENAME CONSTRAINT ck_config_seguimiento_dia_mensual TO ck_config_proyecto_dia_mensual;

-- Indices independientes de configuracion_proyecto (creados en V4).
-- Los indices asociados a PK/UNIQUE se renombran automaticamente junto a sus restricciones.
ALTER INDEX idx_config_seguimiento_responsable_semanal
    RENAME TO idx_config_proyecto_responsable_semanal;
ALTER INDEX idx_config_seguimiento_responsable_mensual
    RENAME TO idx_config_proyecto_responsable_mensual;

-- Restricciones de evidencia_taller (creadas en V9).
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_pkey TO evidencia_taller_pkey;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_id_clase_taller_fkey TO evidencia_taller_id_clase_taller_fkey;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_id_archivo_plataforma_key TO evidencia_taller_id_archivo_plataforma_key;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_id_archivo_plataforma_check TO evidencia_taller_id_archivo_plataforma_check;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_nombre_fisico_check TO evidencia_taller_nombre_fisico_check;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_mime_type_check TO evidencia_taller_mime_type_check;
ALTER TABLE evidencia_taller
    RENAME CONSTRAINT evidencia_tamanio_bytes_check TO evidencia_taller_tamanio_bytes_check;

-- Indice independiente de evidencia_taller (creado en V9).
ALTER INDEX idx_evidencia_clase RENAME TO idx_evidencia_taller_clase;
