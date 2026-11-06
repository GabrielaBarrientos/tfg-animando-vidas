package org.tfg.backend.seguimiento;

import org.tfg.backend.proyecto.EstadoProyecto;

public record ConfiguracionSeguimientoResponse(
        Long idConfiguracionSeguimiento,
        Long idProyecto,
        Integer umbralProximoAtrasoSemanas,
        Integer umbralAtrasoSemanas,
        Long idResponsableSeguimiento,
        DiaInformeSemanal diaInformeSemanal,
        Long idResponsableProyecto,
        TipoDiaInformeMensual tipoDiaInformeMensual,
        Integer diaInformeMensual,
        EstadoProyecto estadoProyecto,
        boolean configurada) {
}
