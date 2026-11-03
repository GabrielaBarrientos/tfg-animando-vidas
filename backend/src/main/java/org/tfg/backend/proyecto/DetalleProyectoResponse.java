package org.tfg.backend.proyecto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DetalleProyectoResponse(
        ProyectoResponse proyecto,
        OffsetDateTime ultimaActualizacion,
        PlanificacionResumen planificacion,
        SeguimientoResumen seguimiento,
        long cantidadDocentes) {

    public record PlanificacionResumen(
            LocalDate fechaInicio,
            LocalDate fechaFinalizacion,
            Integer cantidadClases,
            String frecuenciaClases,
            long duracionMeses) {
    }

    public record SeguimientoResumen(
            Integer umbralProximoAtrasoSemanas,
            Integer umbralAtrasoSemanas,
            String responsableSeguimiento,
            String diaInformeSemanal,
            String responsableProyecto,
            String tipoDiaInformeMensual,
            Integer diaInformeMensual) {
    }
}
