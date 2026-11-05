package org.tfg.backend.integration.educationalplatform;

import java.time.LocalDate;
import java.util.List;

/** Contrato de GET /taller; independiente de la consulta general GET /proyecto. */
public record PlatformDetalleTallerResponse(
        Integer id_taller,
        List<ClaseDetallePlataforma> clases) {

    public Integer idTaller() {
        return id_taller;
    }

    public record ClaseDetallePlataforma(
            Long idClasePlataforma,
            Integer numeroClase,
            String nombreClase,
            String nombreModulo,
            String estadoClase,
            LocalDate fechaRealizacion,
            Integer cantidadParticipantes,
            Integer cantidadEvidencias) { }
}
