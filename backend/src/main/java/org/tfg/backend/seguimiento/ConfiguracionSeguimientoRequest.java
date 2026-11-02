package org.tfg.backend.seguimiento;

public record ConfiguracionSeguimientoRequest(
        Integer umbralProximoAtrasoSemanas,
        Integer umbralAtrasoSemanas,
        Long idResponsableSeguimiento,
        DiaInformeSemanal diaInformeSemanal,
        Long idResponsableProyecto,
        TipoDiaInformeMensual tipoDiaInformeMensual,
        Integer diaInformeMensual) {
}
