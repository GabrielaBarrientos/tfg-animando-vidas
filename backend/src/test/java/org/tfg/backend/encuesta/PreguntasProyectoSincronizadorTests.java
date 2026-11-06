package org.tfg.backend.encuesta;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.PlatformTallerResponse;
import org.tfg.backend.proyecto.Proyecto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreguntasProyectoSincronizadorTests {
    private final PreguntaProyectoRepository repositorio = mock(PreguntaProyectoRepository.class);
    private final PreguntasProyectoSincronizador sincronizador = new PreguntasProyectoSincronizador(repositorio);

    private PlatformTallerResponse.PreguntaPlataforma pregunta(Long id, String enunciado, String tipo, int orden) {
        return new PlatformTallerResponse.PreguntaPlataforma(id, enunciado, tipo, orden);
    }

    private PlatformTallerResponse taller(String id, long idClase1, long idClase2, String texto) {
        return new PlatformTallerResponse(id, "Proyecto", (short) 2026, "Empresa", "usuario.uno",
                List.of(new PlatformTallerResponse.ClasePlataforma(idClase1, 1, null, null, 0,
                                List.of(pregunta(101L, "Introducción", "introducción", 1),
                                        pregunta(102L, texto, "clase", 2))),
                        new PlatformTallerResponse.ClasePlataforma(idClase2, 2, null, null, 0,
                                List.of(pregunta(103L, texto, "clase", 1)))));
    }

    @Test
    void admiteCatalogoCoherenteEntreTalleresConDistintosIdsDeClases() {
        sincronizador.validarCatalogo(List.of(
                taller("8", 7, 8, "¿Cómo resultó la clase?"),
                taller("9", 17, 18, "¿Cómo resultó la clase?")));
    }

    @Test
    void mismoEnunciadoEnDistintasClasesTieneIdentidadDistinta() {
        Proyecto proyecto = mock(Proyecto.class);
        when(proyecto.getIdProyecto()).thenReturn(1L);
        when(repositorio.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(1L)).thenReturn(List.of());
        PlatformTallerResponse primero = taller("8", 7, 8, "¿Cómo resultó la clase?");
        sincronizador.validarCatalogo(List.of(primero));
        sincronizador.guardarCatalogo(proyecto, List.of(primero));
        verify(repositorio, times(3)).save(any(PreguntaProyecto.class));
    }

    @Test
    void rechazaCatalogosInconsistentesSinGuardar() {
        try {
            sincronizador.validarCatalogo(List.of(taller("8", 7, 8, "Pregunta 1"),
                    taller("9", 17, 18, "Pregunta 2")));
            fail("Se esperaba rechazo del catálogo inconsistente");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaReutilizacionDeIdEntreClases() {
        PlatformTallerResponse dato = new PlatformTallerResponse("8", "Proyecto", (short) 2026,
                "Empresa", "usuario.uno", List.of(
                new PlatformTallerResponse.ClasePlataforma(7L, 1, null, null, 0,
                        List.of(pregunta(101L, "Pregunta", "clase", 1))),
                new PlatformTallerResponse.ClasePlataforma(8L, 2, null, null, 0,
                        List.of(pregunta(101L, "Pregunta", "clase", 1)))));
        try {
            sincronizador.validarCatalogo(List.of(dato));
            fail("Se esperaba rechazo del identificador repetido");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaOrdenNoConsecutivo() {
        PlatformTallerResponse dato = new PlatformTallerResponse("8", "Proyecto", (short) 2026,
                "Empresa", "usuario.uno", List.of(
                new PlatformTallerResponse.ClasePlataforma(7L, 1, null, null, 0,
                        List.of(pregunta(101L, "Pregunta", "clase", 2)))));
        try {
            sincronizador.validarCatalogo(List.of(dato));
            fail("Se esperaba rechazo del orden inválido");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
    }

    @Test
    void conservaIdentidadInternaCuandoActualizaDefinicion() {
        Proyecto proyecto = mock(Proyecto.class);
        when(proyecto.getIdProyecto()).thenReturn(1L);
        PreguntaProyecto existente = new PreguntaProyecto(proyecto, 102L, "Texto anterior", TipoPregunta.CLASE, 1);
        when(repositorio.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(1L))
                .thenReturn(List.of(existente));
        sincronizador.guardarCatalogo(proyecto, List.of(taller("8", 7, 8, "Pregunta nueva")));
        verify(repositorio).save(existente);
        assertEquals("Pregunta nueva", existente.getEnunciado());
        assertEquals(Integer.valueOf(2), existente.getOrden());
        assertEquals(TipoPregunta.CLASE, existente.getTipo());
    }
}
