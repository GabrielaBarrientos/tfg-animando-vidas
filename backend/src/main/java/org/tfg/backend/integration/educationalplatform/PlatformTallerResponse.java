package org.tfg.backend.integration.educationalplatform;

import java.time.LocalDate;
import java.util.List;

/** GET /talleres: ejecución de todas las clases de todos los talleres. */
public record PlatformTallerResponse(
        String idTallerPlataforma, String nombreProyecto, Short anioProyecto,
        String nombreEmpresa, String usuarioPlataforma, List<ClasePlataforma> clases) {
    public record ClasePlataforma(Long idClasePlataforma, Integer numeroClase,
            LocalDate fechaRealizacion, Integer cantidadParticipantes,
            Integer cantidadEvidencias, String estado) { }
}
