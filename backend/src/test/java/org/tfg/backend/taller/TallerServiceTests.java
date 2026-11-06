package org.tfg.backend.taller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.programa.ProgramaRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.seguimiento.ConfiguracionSeguimientoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TallerServiceTests {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final DocenteProyectoRepository docentes = mock(DocenteProyectoRepository.class);
    private final TallerRepository talleres = mock(TallerRepository.class);
    private final ClaseTallerRepository clases = mock(ClaseTallerRepository.class);
    private final PlanificacionProyectoRepository planes = mock(PlanificacionProyectoRepository.class);
    private final ConfiguracionSeguimientoRepository seguimiento = mock(ConfiguracionSeguimientoRepository.class);
    private final EducationalPlatformClient plataforma = mock(EducationalPlatformClient.class);
    private final ProgramaRepository programas = mock(ProgramaRepository.class);
    private final TallerService servicio = new TallerService(proyectos, usuarios, docentes, talleres,
            clases, planes, seguimiento, plataforma, new TallerSeguimientoCalculador(), programas);
    private final OidcUser google = mock(OidcUser.class);
    private final Proyecto proyecto = mock(Proyecto.class);

    @BeforeEach
    void preparar() {
        Usuario usuario = mock(Usuario.class);
        when(google.getSubject()).thenReturn("sub-test");
        when(usuarios.findByGoogleSub("sub-test")).thenReturn(Optional.of(usuario));
        when(usuario.isHabilitado()).thenReturn(true);
        when(usuario.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(proyectos.findById(1L)).thenReturn(Optional.of(proyecto));
    }

    @Test
    void impideSincronizarTalleresSinPrograma() {
        when(proyecto.isProgramaSincronizado()).thenReturn(false);
        try {
            servicio.sincronizar(1L, google);
            fail("Se esperaba rechazo cuando falta el programa");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verifyNoInteractions(plataforma);
    }

    @Test
    void consultaMetadatosSinLlamarPlataforma() {
        when(proyecto.getEstado()).thenReturn(org.tfg.backend.proyecto.EstadoProyecto.PLANIFICADO);
        TalleresMetadatosResponse resultado = servicio.obtenerMetadatos(1L, google);
        assertEquals("PLANIFICADO", resultado.estadoProyecto());
        verifyNoInteractions(plataforma);
    }
}
