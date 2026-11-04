package org.tfg.backend.taller;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tfg.backend.docente.Docente;
import org.tfg.backend.docente.DocenteProyecto;
import org.tfg.backend.planificacion.FrecuenciaClases;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.seguimiento.ConfiguracionSeguimiento;
import org.tfg.backend.taller.TallerResumenResponse.EstadoEvidencias;
import org.tfg.backend.taller.TallerResumenResponse.EstadoTaller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TallerSeguimientoCalculadorTests {
    private final TallerSeguimientoCalculador calculador = new TallerSeguimientoCalculador();

    private Taller taller() {
        Taller taller = mock(Taller.class);
        DocenteProyecto asociacion = mock(DocenteProyecto.class);
        Docente docente = mock(Docente.class);
        when(taller.getIdTallerPlataforma()).thenReturn("t-1");
        when(taller.getAsignacion()).thenReturn(asociacion);
        when(asociacion.getDocente()).thenReturn(docente);
        when(asociacion.getCantidadAlumnos()).thenReturn(15);
        when(asociacion.getPais()).thenReturn("Argentina");
        when(asociacion.getCentroEducativo()).thenReturn("Escuela 1");
        when(asociacion.getUsuarioPlataforma()).thenReturn("ana.perez");
        return taller;
    }

    private PlanificacionProyecto plan() {
        PlanificacionProyecto plan = mock(PlanificacionProyecto.class);
        when(plan.getCantidadClases()).thenReturn(4);
        when(plan.getFechaInicio()).thenReturn(LocalDate.of(2026, 7, 1));
        when(plan.getFrecuenciaClases()).thenReturn(FrecuenciaClases.MENSUAL);
        return plan;
    }

    private ConfiguracionSeguimiento seguimiento() {
        ConfiguracionSeguimiento seguimiento = mock(ConfiguracionSeguimiento.class);
        when(seguimiento.getUmbralProximoAtrasoSemanas()).thenReturn(2);
        when(seguimiento.getUmbralAtrasoSemanas()).thenReturn(3);
        return seguimiento;
    }

    private List<ClaseTaller> dosClasesRealizadas() {
        ClaseTaller primera = mock(ClaseTaller.class);
        when(primera.getNumeroClase()).thenReturn(1);
        when(primera.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 8, 3));
        when(primera.getCantidadEvidencias()).thenReturn(1);
        ClaseTaller segunda = mock(ClaseTaller.class);
        when(segunda.getNumeroClase()).thenReturn(2);
        when(segunda.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 8, 31));
        when(segunda.getCantidadEvidencias()).thenReturn(1);
        return List.of(primera, segunda);
    }

    @Test
    void fechasLimiteSonIntervalosFijosDeDias() {
        LocalDate inicio = LocalDate.of(2026, 7, 1);
        assertEquals(LocalDate.of(2026, 7, 31), calculador.calcularFechaLimite(inicio, FrecuenciaClases.MENSUAL, 1));
        assertEquals(LocalDate.of(2026, 9, 29), calculador.calcularFechaLimite(inicio, FrecuenciaClases.MENSUAL, 3));
        assertEquals(LocalDate.of(2026, 7, 31), calculador.calcularFechaLimite(inicio, FrecuenciaClases.QUINCENAL, 2));
        assertEquals(LocalDate.of(2026, 7, 22), calculador.calcularFechaLimite(inicio, FrecuenciaClases.SEMANAL, 3));
    }

    @Test
    void proximoAtrasoConVeinteDiasTranscurridos() {
        TallerResumenResponse resultado = calculador.calcular(taller(), dosClasesRealizadas(),
                plan(), seguimiento(), LocalDate.of(2026, 10, 19));
        assertEquals(EstadoTaller.PROXIMO_ATRASO, resultado.estado());
        assertEquals(3, resultado.avanceEsperado());
        assertEquals(2, resultado.avanceReal());
        assertEquals(1, resultado.diferencia());
        assertEquals("ana.perez", resultado.docente());
    }

    @Test
    void atrasoConVeintiunDiasDesdeFechaLimite() {
        TallerResumenResponse resultado = calculador.calcular(taller(), dosClasesRealizadas(),
                plan(), seguimiento(), LocalDate.of(2026, 10, 20));
        assertEquals(EstadoTaller.ATRASADO, resultado.estado());
    }

    @Test
    void diferenciaNuncaEsNegativaSiAvanceRealSuperaAlEsperado() {
        TallerResumenResponse resultado = calculador.calcular(taller(), dosClasesRealizadas(),
                plan(), seguimiento(), LocalDate.of(2026, 7, 2));
        assertEquals(0, resultado.avanceEsperado());
        assertEquals(2, resultado.avanceReal());
        assertEquals(0, resultado.diferencia());
    }

    @Test
    void sinClasesRealizadasNoExigeEvidencias() {
        TallerResumenResponse resultado = calculador.calcular(taller(), List.of(), plan(), seguimiento(),
                LocalDate.of(2026, 7, 2));
        assertEquals(EstadoTaller.NO_INICIADO, resultado.estado());
        assertEquals(EstadoEvidencias.SIN_CLASES_REALIZADAS, resultado.evidencias());
        assertEquals(0, resultado.avanceEsperado());
    }

    @Test
    void evidenciaPendienteSiUnaClaseRealizadaNoTieneFotos() {
        ClaseTaller tercera = mock(ClaseTaller.class);
        when(tercera.getNumeroClase()).thenReturn(3);
        when(tercera.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 11, 20));
        when(tercera.getCantidadEvidencias()).thenReturn(0);
        TallerResumenResponse resultado = calculador.calcular(taller(), List.of(tercera),
                plan(), seguimiento(), LocalDate.of(2026, 11, 21));
        assertEquals(EstadoEvidencias.PENDIENTES, resultado.evidencias());
    }
}
