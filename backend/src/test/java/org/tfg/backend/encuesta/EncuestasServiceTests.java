package org.tfg.backend.encuesta;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.EstadoClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;
import org.tfg.backend.docente.DocenteProyecto;
import org.tfg.backend.proyecto.Proyecto;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EncuestasServiceTests {
    private final TallerService talleres = mock(TallerService.class);
    private final ClaseTallerRepository clases = mock(ClaseTallerRepository.class);
    private final PreguntaProyectoRepository preguntas = mock(PreguntaProyectoRepository.class);
    private final RespuestaTallerRepository respuestas = mock(RespuestaTallerRepository.class);
    private final EducationalPlatformClient plataforma = mock(EducationalPlatformClient.class);
    private final OidcUser usuario = mock(OidcUser.class);
    private final Taller taller = mock(Taller.class);
    private final EncuestasService servicio = new EncuestasService(talleres, clases, preguntas, respuestas, plataforma);

    @Test
    void consultarNoSincronizaAutomaticamente() {
        when(talleres.buscarTallerAutorizado(1L, "8", usuario)).thenReturn(taller);
        when(taller.getIdTaller()).thenReturn(4L);
        EncuestasTallerResponse resultado = servicio.consultar(1L, "8", usuario);
        assertTrue(resultado.clases().isEmpty());
        verifyNoInteractions(plataforma);
    }

    @Test
    void rechazaSincronizarSinPrograma() {
        when(talleres.buscarTallerAutorizado(1L, "8", usuario)).thenReturn(taller);
        DocenteProyecto asignacion = mock(DocenteProyecto.class);
        Proyecto proyecto = mock(Proyecto.class);
        when(taller.getAsignacion()).thenReturn(asignacion);
        when(asignacion.getProyecto()).thenReturn(proyecto);
        when(proyecto.isProgramaSincronizado()).thenReturn(false);
        try {
            servicio.sincronizar(1L, "8", usuario);
            fail("Se esperaba rechazo cuando falta el programa");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verifyNoInteractions(plataforma);
    }

    @Test
    void introduccionNoSeExponeEnPantalla14() {
        when(talleres.buscarTallerAutorizado(1L, "8", usuario)).thenReturn(taller);
        when(taller.getIdTaller()).thenReturn(4L);
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getIdClaseTaller()).thenReturn(5L);
        when(clase.getNumeroClase()).thenReturn(1);
        when(clase.getNombreClase()).thenReturn("Encuentro 1");
        when(clase.getEstadoClase()).thenReturn(EstadoClaseTaller.REALIZADA);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8")).thenReturn(List.of(clase));
        PreguntaProyecto pregunta = mock(PreguntaProyecto.class);
        when(pregunta.getTipo()).thenReturn(TipoPregunta.INTRODUCCION);
        RespuestaTaller respuesta = mock(RespuestaTaller.class);
        when(respuesta.getPregunta()).thenReturn(pregunta);
        when(respuestas.buscarPorTaller(4L)).thenReturn(List.of(respuesta));
        EncuestasTallerResponse resultado = servicio.consultar(1L, "8", usuario);
        assertTrue(resultado.clases().get(0).respuestas().isEmpty());
    }
}
