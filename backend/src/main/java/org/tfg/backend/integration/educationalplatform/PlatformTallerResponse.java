package org.tfg.backend.integration.educationalplatform;

import java.time.LocalDate;
import java.util.List;

/** Contrato de GET /proyecto: cada clase contiene sus preguntas. */
public record PlatformTallerResponse(
        String idTaller,
        String nombreProyecto,
        Short anioProyecto,
        String nombreEmpresa,
        String usuarioPlataforma,
        List<ClasePlataforma> clases) {

    public record PreguntaPlataforma(Long idPreguntaPlataforma, String enunciado,
            String tipo, Integer orden) { }

    public record ClasePlataforma(Long idClasePlataforma, Integer numeroClase,
            LocalDate fechaRealizacion, Integer cantidadParticipantes,
            Integer cantidadEvidencias, List<PreguntaPlataforma> preguntas) {
        /** Compatibilidad con las pruebas existentes de clases sin catálogo. */
        public ClasePlataforma(Long idClasePlataforma, Integer numeroClase,
                LocalDate fechaRealizacion, Integer cantidadParticipantes,
                Integer cantidadEvidencias) {
            this(idClasePlataforma, numeroClase, fechaRealizacion,
                    cantidadParticipantes, cantidadEvidencias, List.of());
        }
    }
}
