package org.tfg.backend.encuesta;

import java.time.OffsetDateTime;
import java.util.List;

public record EncuestasTallerResponse(OffsetDateTime ultimaSincronizacion, List<Clase> clases) {
    public record Clase(Long idClaseTaller, Integer numeroClase, String nombreClase,
            String estado, List<PreguntaRespuesta> respuestas) { }
    public record PreguntaRespuesta(Long idPreguntaProyecto, String pregunta, String tipo,
            String respuesta) { }
}
