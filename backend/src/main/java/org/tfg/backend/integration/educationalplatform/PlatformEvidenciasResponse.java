package org.tfg.backend.integration.educationalplatform;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Metadatos de GET /evidencias, sin rutas físicas ni fechas de la plataforma. */
public record PlatformEvidenciasResponse(
        Integer id_taller, List<ArchivoPlataforma> evidencias) {
    public record ArchivoPlataforma(
            Long id_archivo_plataforma,
            Long id_clase_plataforma,
            String nombre_fisico,
            String mime_type,
            @JsonProperty("tamaño") Long tamanio) { }
}
