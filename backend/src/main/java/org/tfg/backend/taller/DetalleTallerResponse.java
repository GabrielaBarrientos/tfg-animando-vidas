package org.tfg.backend.taller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.tfg.backend.taller.TallerResumenResponse.EstadoEvidencias;
import org.tfg.backend.taller.TallerResumenResponse.EstadoTaller;

/** Detalle de un taller almacenado en PostgreSQL; no consulta la plataforma. */
public record DetalleTallerResponse(
        String idTaller,
        String nombreDocente,
        String usuarioPlataforma,
        String pais,
        String provincia,
        String centroEducativo,
        int cantidadAlumnos,
        int totalClases,
        int avanceEsperado,
        int avanceReal,
        int diferencia,
        EstadoTaller estado,
        EstadoEvidencias evidencias,
        boolean pendienteBaja,
        OffsetDateTime ultimaSincronizacion,
        List<ClaseDetalle> clases) {

    public record ClaseDetalle(
            Long idClaseTaller,
            int numeroClase,
            String nombreClase,
            String nombreModulo,
            LocalDate fechaPrevista,
            LocalDate fechaRealizacion,
            Integer cantidadParticipantes,
            int cantidadEvidencias,
            EstadoClase estado) { }

    public enum EstadoClase { REALIZADA, PENDIENTE }
}
