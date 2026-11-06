package org.tfg.backend.programa;

import java.time.OffsetDateTime;
import java.util.List;

public record ProgramaResponse(boolean sincronizado, OffsetDateTime ultimaSincronizacion,
        List<Clase> clases) {
    public record Clase(Long idPrograma, Long idClasePlataforma, int numeroClase,
            String nombreClase, String nombreModulo, List<Pregunta> preguntas) { }
    public record Pregunta(Long idPreguntaProyecto, Long idPreguntaPlataforma,
            int numeroPregunta, String tipo, String enunciado) { }
}
