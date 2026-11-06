package org.tfg.backend.programa;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.encuesta.PreguntaProyectoRepository;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformProgramaResponse;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.UsuarioRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgramaServiceTests {
    private final ProgramaService servicio = new ProgramaService(mock(ProgramaRepository.class),
            mock(PreguntaProyectoRepository.class), mock(ProyectoRepository.class),
            mock(UsuarioRepository.class), mock(EducationalPlatformClient.class));

    private PlatformProgramaResponse remoto(String formato, String tipo, int orden) {
        return new PlatformProgramaResponse(List.of(new PlatformProgramaResponse.ClasePrograma(
                7L, 1, "Encuentro 1", "Dibujo", List.of(
                        new PlatformProgramaResponse.PreguntaPrograma(101L, orden, formato, tipo, "Pregunta")))));
    }

    @Test
    void aceptaPreguntasIntroDeTexto() {
        servicio.validar(remoto("texto", "intro", 1));
    }

    @Test
    void rechazaFormatoNoTextual() {
        try {
            servicio.validar(remoto("numero", "clase", 1));
            fail("Se esperaba rechazo de formato numérico");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
            assertTrue(error.getReason().contains("101"));
        }
    }

    @Test
    void rechazaOrdenNoConsecutivo() {
        try {
            servicio.validar(remoto("texto", "clase", 2));
            fail("Se esperaba rechazo de orden no consecutivo");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
    }

    @Test
    void rechazaTipoDesconocido() {
        try {
            servicio.validar(remoto("texto", "evaluacion", 1));
            fail("Se esperaba rechazo de tipo desconocido");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
    }
}
