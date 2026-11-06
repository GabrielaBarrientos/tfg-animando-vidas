package org.tfg.backend.integration.educationalplatform;

import java.util.List;

/** Respuestas identificadas explícitamente por taller, clase y pregunta. */
public record PlatformEncuestasResponse(Integer idTallerPlataforma, List<Respuesta> respuestas) {
    public record Respuesta(Integer idTallerPlataforma, Long idClasePlataforma,
            Long idPreguntaPlataforma, String respuesta) { }
}
