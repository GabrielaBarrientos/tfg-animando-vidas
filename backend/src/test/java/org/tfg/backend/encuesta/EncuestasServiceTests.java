package org.tfg.backend.encuesta;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformEncuestasResponse;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EncuestasServiceTests {
    private final TallerService talleres = mock(TallerService.class);
    private final ClaseTallerRepository clases = mock(ClaseTallerRepository.class);
    private final PreguntaProyectoRepository preguntas = mock(PreguntaProyectoRepository.class);
    private final EncuestaTallerRepository encuestas = mock(EncuestaTallerRepository.class);
    private final RespuestaEncuestaRepository respuestas = mock(RespuestaEncuestaRepository.class);
    private final EducationalPlatformClient plataforma = mock(EducationalPlatformClient.class);
    private final OidcUser google = mock(OidcUser.class);
    private final Taller taller = mock(Taller.class);
    private final EncuestasService servicio = new EncuestasService(talleres, clases, preguntas,
            encuestas, respuestas, plataforma);

    @BeforeEach
    void preparar() {
        when(talleres.buscarTallerAutorizado(1L, "8", google)).thenReturn(taller);
        when(taller.getIdTaller()).thenReturn(15L);
        when(taller.getIdTallerPlataforma()).thenReturn("8");
        when(encuestas.save(any(EncuestaTaller.class))).thenAnswer(org.mockito.AdditionalAnswers.returnsFirstArg());
        when(respuestas.save(any(RespuestaEncuesta.class))).thenAnswer(org.mockito.AdditionalAnswers.returnsFirstArg());
    }

    @Test
    void consultaInicialNoContactaLaPlataforma() {
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of());
        EncuestasTallerResponse resultado = servicio.consultar(1L, "8", google);
        assertTrue(resultado.clases().isEmpty());
        assertNull(resultado.ultimaSincronizacion());
        verifyNoInteractions(plataforma);
    }

    @Test
    void rechazaRespuestaParaClaseNoSincronizada() {
        when(plataforma.obtenerEncuestas(8)).thenReturn(new PlatformEncuestasResponse(8,
                List.of(new PlatformEncuestasResponse.Respuesta(8, 7L, 101L, "Respuesta"))));
        try {
            servicio.sincronizar(1L, "8", google);
            fail("Se esperaba rechazo por clase faltante");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verify(respuestas, never()).save(any(RespuestaEncuesta.class));
        verify(taller, never()).registrarSincronizacionEncuestas(any(OffsetDateTime.class));
    }

    @Test
    void guardaRespuestaSinModificarOtrasClases() {
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getIdClasePlataforma()).thenReturn(7L);
        when(clase.getIdClaseTaller()).thenReturn(25L);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        PreguntaProyecto pregunta = mock(PreguntaProyecto.class);
        when(pregunta.getIdPreguntaPlataforma()).thenReturn(101L);
        when(preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(1L))
                .thenReturn(List.of(pregunta));
        when(plataforma.obtenerEncuestas(8)).thenReturn(new PlatformEncuestasResponse(8,
                List.of(new PlatformEncuestasResponse.Respuesta(8, 7L, 101L, "Bien"))));
        assertEquals(1, servicio.sincronizar(1L, "8", google));
        verify(encuestas).save(any(EncuestaTaller.class));
        verify(respuestas).save(any(RespuestaEncuesta.class));
        verify(taller).registrarSincronizacionEncuestas(any(OffsetDateTime.class));
    }

    @Test
    void rechazaIdentidadesDuplicadasAntesDeGuardar() {
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getIdClasePlataforma()).thenReturn(7L);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        PreguntaProyecto pregunta = mock(PreguntaProyecto.class);
        when(pregunta.getIdPreguntaPlataforma()).thenReturn(101L);
        when(preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(1L))
                .thenReturn(List.of(pregunta));
        when(plataforma.obtenerEncuestas(8)).thenReturn(new PlatformEncuestasResponse(8,
                List.of(new PlatformEncuestasResponse.Respuesta(8, 7L, 101L, "Uno"),
                        new PlatformEncuestasResponse.Respuesta(8, 7L, 101L, "Dos"))));
        try {
            servicio.sincronizar(1L, "8", google);
            fail("Se esperaba rechazo por identidad duplicada");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(respuestas, never()).save(any(RespuestaEncuesta.class));
    }

    @Test
    void noExponeIntroduccionPeroConservaRespuestaParaAnalisis() {
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getIdClaseTaller()).thenReturn(25L);
        when(clase.getNumeroClase()).thenReturn(1);
        when(clase.getEstadoClase()).thenReturn(org.tfg.backend.taller.EstadoClaseTaller.REALIZADA);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        EncuestaTaller encuesta = mock(EncuestaTaller.class);
        when(encuesta.getClase()).thenReturn(clase);
        PreguntaProyecto introduccion = mock(PreguntaProyecto.class);
        when(introduccion.getTipo()).thenReturn(TipoPregunta.INTRODUCCION);
        PreguntaProyecto deClase = mock(PreguntaProyecto.class);
        when(deClase.getTipo()).thenReturn(TipoPregunta.CLASE);
        when(deClase.getEnunciado()).thenReturn("¿Cómo resultó la clase?");
        when(deClase.getIdPreguntaProyecto()).thenReturn(32L);
        RespuestaEncuesta respuestaIntroduccion = mock(RespuestaEncuesta.class);
        when(respuestaIntroduccion.getEncuesta()).thenReturn(encuesta);
        when(respuestaIntroduccion.getPregunta()).thenReturn(introduccion);
        RespuestaEncuesta respuestaClase = mock(RespuestaEncuesta.class);
        when(respuestaClase.getEncuesta()).thenReturn(encuesta);
        when(respuestaClase.getPregunta()).thenReturn(deClase);
        when(respuestaClase.getValorRespuesta()).thenReturn("Muy bien");
        when(respuestas.buscarPorTaller(15L)).thenReturn(List.of(respuestaIntroduccion, respuestaClase));

        EncuestasTallerResponse resultado = servicio.consultar(1L, "8", google);
        assertEquals(1, resultado.clases().size());
        assertEquals(1, resultado.clases().get(0).respuestas().size());
        assertEquals("¿Cómo resultó la clase?", resultado.clases().get(0).respuestas().get(0).pregunta());
        assertEquals("Muy bien", resultado.clases().get(0).respuestas().get(0).respuesta());
        verifyNoInteractions(plataforma);
    }

    @Test
    void rechazaRespuestaDeOtroTallerAntesDeGuardar() {
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getIdClasePlataforma()).thenReturn(7L);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        PreguntaProyecto pregunta = mock(PreguntaProyecto.class);
        when(pregunta.getIdPreguntaPlataforma()).thenReturn(101L);
        when(preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(1L))
                .thenReturn(List.of(pregunta));
        when(plataforma.obtenerEncuestas(8)).thenReturn(new PlatformEncuestasResponse(8,
                List.of(new PlatformEncuestasResponse.Respuesta(9, 7L, 101L, "Incorrecta"))));
        try {
            servicio.sincronizar(1L, "8", google);
            fail("Se esperaba rechazo de respuesta de otro taller");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
        verify(respuestas, never()).save(any(RespuestaEncuesta.class));
    }
}
