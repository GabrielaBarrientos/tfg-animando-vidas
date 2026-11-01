
package org.tfg.backend.planificacion;

import java.time.LocalDate;

public record PlanificacionProyectoResponse(
        Long idPlanificacionProyecto,
        Long idProyecto,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        Integer cantidadClases,
        FrecuenciaClases frecuenciaClases,
        long duracionMeses
    ) {
}
