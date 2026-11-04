package org.tfg.backend.taller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;
import org.tfg.backend.planificacion.FrecuenciaClases;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.seguimiento.ConfiguracionSeguimiento;
import org.tfg.backend.taller.TallerResumenResponse.EstadoEvidencias;
import org.tfg.backend.taller.TallerResumenResponse.EstadoTaller;

/** Fechas límite: inicio + número de clase por intervalo fijo (7, 15 o 30 días). */
@Component
public class TallerSeguimientoCalculador {
    public LocalDate calcularFechaLimite(LocalDate inicio, FrecuenciaClases frecuencia, int numeroClase) {
        if (numeroClase < 1) throw new IllegalArgumentException("El número de clase debe ser positivo");
        int dias;
        if (frecuencia == FrecuenciaClases.SEMANAL) dias = 7;
        else if (frecuencia == FrecuenciaClases.QUINCENAL) dias = 15;
        else if (frecuencia == FrecuenciaClases.MENSUAL) dias = 30;
        else throw new IllegalArgumentException("Frecuencia de clases inválida");
        return inicio.plusDays((long) dias * numeroClase);
    }

    public TallerResumenResponse calcular(Taller taller, List<ClaseTaller> clases,
            PlanificacionProyecto planificacion, ConfiguracionSeguimiento configuracion, LocalDate hoy) {
        int total = planificacion.getCantidadClases();
        boolean[] realizadasPorNumero = new boolean[total + 1];
        int realizadas = 0;
        int conEvidencias = 0;
        for (ClaseTaller clase : clases) {
            int numero = clase.getNumeroClase();
            if (numero >= 1 && numero <= total && clase.getFechaRealizacion() != null
                    && !realizadasPorNumero[numero]) {
                realizadasPorNumero[numero] = true;
                realizadas++;
                if (clase.getCantidadEvidencias() > 0) conEvidencias++;
            }
        }
        int esperado = 0;
        int semanasMaximasDeAtraso = 0;
        for (int numero = 1; numero <= total; numero++) {
            LocalDate limite = calcularFechaLimite(planificacion.getFechaInicio(),
                    planificacion.getFrecuenciaClases(), numero);
            if (!limite.isAfter(hoy)) {
                esperado++;
                if (!realizadasPorNumero[numero]) {
                    long diasAtraso = ChronoUnit.DAYS.between(limite, hoy);
                    int semanasCompletas = (int) (diasAtraso / 7);
                    if (semanasCompletas > semanasMaximasDeAtraso) semanasMaximasDeAtraso = semanasCompletas;
                }
            }
        }
        EstadoTaller estado;
        if (semanasMaximasDeAtraso >= configuracion.getUmbralAtrasoSemanas()) {
            estado = EstadoTaller.ATRASADO;
        } else if (semanasMaximasDeAtraso >= configuracion.getUmbralProximoAtrasoSemanas()) {
            estado = EstadoTaller.PROXIMO_ATRASO;
        } else if (realizadas == 0) {
            estado = EstadoTaller.NO_INICIADO;
        } else {
            estado = EstadoTaller.EN_PLAZO;
        }
        EstadoEvidencias evidencias;
        if (realizadas == 0) evidencias = EstadoEvidencias.SIN_CLASES_REALIZADAS;
        else if (conEvidencias == realizadas) evidencias = EstadoEvidencias.COMPLETAS;
        else evidencias = EstadoEvidencias.PENDIENTES;
        return new TallerResumenResponse(taller.getIdTallerPlataforma(),
                taller.getAsignacion().getPais(), taller.getAsignacion().getCentroEducativo(),
                taller.getAsignacion().getUsuarioPlataforma(),
                taller.getAsignacion().getCantidadAlumnos(), esperado, realizadas,
                Math.max(0, esperado - realizadas), estado, evidencias, taller.isPendienteBaja());
    }
}
