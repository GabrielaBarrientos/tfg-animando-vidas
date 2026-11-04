package org.tfg.backend.proyecto;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.tfg.backend.empresa.Empresa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProyectoEstadoTalleresTests {
    @Test
    void sincronizacionYBajaConfirmanEstadosSinPerderLaFecha() {
        Proyecto proyecto = new Proyecto("Ejemplo", (short) 2026, null, mock(Empresa.class));
        proyecto.marcarPlanificado();
        proyecto.actualizarEstadoPorTalleres(false);
        assertEquals(EstadoProyecto.PLANIFICADO, proyecto.getEstado());

        proyecto.registrarSincronizacionTalleres(2);
        OffsetDateTime fecha = proyecto.getUltimaSincronizacionTalleres();
        assertNotNull(fecha);
        assertEquals(2, proyecto.getCantidadTalleresPlataforma());
        proyecto.actualizarEstadoPorTalleres(true);
        assertEquals(EstadoProyecto.EN_CURSO, proyecto.getEstado());

        proyecto.registrarSincronizacionTalleres(0);
        proyecto.actualizarEstadoPorTalleres(true); // La baja no fue confirmada.
        assertEquals(EstadoProyecto.EN_CURSO, proyecto.getEstado());
        assertEquals(0, proyecto.getCantidadTalleresPlataforma());
        proyecto.actualizarEstadoPorTalleres(false); // Se confirmó la última baja.
        assertEquals(EstadoProyecto.PLANIFICADO, proyecto.getEstado());
        assertNotNull(proyecto.getUltimaSincronizacionTalleres());
    }

    @Test
    void noModificaEstadoDeProyectoSinPlanificar() {
        Proyecto proyecto = new Proyecto("Ejemplo", (short) 2026, null, mock(Empresa.class));
        proyecto.actualizarEstadoPorTalleres(true);
        assertEquals(EstadoProyecto.PENDIENTE_PLANIFICACION, proyecto.getEstado());
    }

    @Test
    void rechazaCantidadNegativa() {
        Proyecto proyecto = new Proyecto("Ejemplo", (short) 2026, null, mock(Empresa.class));
        try {
            proyecto.registrarSincronizacionTalleres(-1);
            fail("Se esperaba rechazar una cantidad negativa");
        } catch (IllegalArgumentException esperado) {
            assertNotNull(esperado.getMessage());
        }
        assertNull(proyecto.getUltimaSincronizacionTalleres());
    }
}
