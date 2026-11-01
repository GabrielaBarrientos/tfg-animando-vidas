
package org.tfg.backend.planificacion;

import java.time.LocalDate;

public record PlanificacionProyectoRequest(
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        Integer cantidadClases,
        FrecuenciaClases frecuenciaClases
    ) {
}
