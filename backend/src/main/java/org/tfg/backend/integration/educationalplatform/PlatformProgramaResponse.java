package org.tfg.backend.integration.educationalplatform;

import java.util.List;

public record PlatformProgramaResponse(List<ClasePrograma> clase) {
    public record ClasePrograma(Long idClasePlataforma, Integer numeroClase,
            String nombreClase, String nombreModulo, List<PreguntaPrograma> pregunta) { }
    public record PreguntaPrograma(Long idPreguntaPlataforma, Integer numeroPregunta,
            String formato, String tipo, String enunciado) { }
}
