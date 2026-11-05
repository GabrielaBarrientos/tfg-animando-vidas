package org.tfg.backend.integration.educationalplatform;

import java.time.LocalDate;
import java.util.List;

/** Contrato de GET /proyecto en la plataforma educativa. */
public record PlatformTallerResponse(
        String idTaller,
        String nombreProyecto,
        Short anioProyecto,
        String nombreEmpresa,
        String usuarioPlataforma,
        List<ClasePlataforma> clases) {
    public record ClasePlataforma(Long idClasePlataforma, Integer numeroClase,
            LocalDate fechaRealizacion, Integer cantidadParticipantes, Integer cantidadEvidencias) { }
}
