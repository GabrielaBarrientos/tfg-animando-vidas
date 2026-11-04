package org.tfg.backend.proyecto;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.seguimiento.ConfiguracionSeguimientoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DetalleProyectoServiceTests {

    private ProyectoRepository proyectoRepository;
    private UsuarioRepository usuarioRepository;
    private PlanificacionProyectoRepository planificacionRepository;
    private ConfiguracionSeguimientoRepository seguimientoRepository;
    private DocenteProyectoRepository docenteRepository;
    private DetalleProyectoService servicio;
    private OidcUser usuarioGoogle;
    private Proyecto proyecto;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        proyectoRepository = mock(ProyectoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        planificacionRepository = mock(PlanificacionProyectoRepository.class);
        seguimientoRepository = mock(ConfiguracionSeguimientoRepository.class);
        docenteRepository = mock(DocenteProyectoRepository.class);
        servicio = new DetalleProyectoService(proyectoRepository, usuarioRepository,
                planificacionRepository, seguimientoRepository, docenteRepository);
        usuarioGoogle = mock(OidcUser.class);
        usuario = mock(Usuario.class);
        proyecto = mock(Proyecto.class);
        Empresa empresa = mock(Empresa.class);
        when(usuarioGoogle.getSubject()).thenReturn("google-test");
        when(usuarioRepository.findByGoogleSub("google-test")).thenReturn(Optional.of(usuario));
        when(usuario.isHabilitado()).thenReturn(true);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyecto.getIdProyecto()).thenReturn(1L);
        when(proyecto.getNombre()).thenReturn("Proyecto de prueba");
        when(proyecto.getEstado()).thenReturn(EstadoProyecto.PLANIFICADO);
        when(proyecto.getAnio()).thenReturn((short) 2026);
        when(proyecto.getEmpresa()).thenReturn(empresa);
        when(empresa.getIdEmpresa()).thenReturn(10L);
        when(empresa.getNombre()).thenReturn("Empresa A");
    }

    @Test
    void administradorConsultaDetalleConDatosNoDisponibles() {
        when(usuario.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(docenteRepository.countByProyecto_IdProyecto(1L)).thenReturn(4L);
        when(docenteRepository.sumarAlumnosPorProyecto(1L)).thenReturn(141L);
        when(docenteRepository.contarEscuelasDistintasPorProyecto(1L)).thenReturn(3L);
        when(docenteRepository.contarPaisesDistintosPorProyecto(1L)).thenReturn(2L);

        DetalleProyectoResponse detalle = servicio.obtener(1L, usuarioGoogle);

        assertEquals(1L, detalle.proyecto().getIdProyecto());
        assertEquals(4L, detalle.cantidadDocentes());
        assertEquals(141L, detalle.cantidadAlumnos());
        assertEquals(3L, detalle.cantidadEscuelas());
        assertEquals(2L, detalle.cantidadPaises());
        assertNull(detalle.planificacion());
        assertNull(detalle.seguimiento());
    }

    @Test
    void proyectoSinDocentesTieneIndicadoresEnCero() {
        when(usuario.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);

        DetalleProyectoResponse detalle = servicio.obtener(1L, usuarioGoogle);

        assertEquals(0L, detalle.cantidadDocentes());
        assertEquals(0L, detalle.cantidadAlumnos());
        assertEquals(0L, detalle.cantidadEscuelas());
        assertEquals(0L, detalle.cantidadPaises());
    }

    @Test
    void operativoPuedeConsultarProyectoAsignado() {
        when(usuario.getRol()).thenReturn(RolUsuario.OPERATIVO);
        Set<Proyecto> asignados = new HashSet<>();
        asignados.add(proyecto);
        when(usuario.getProyectosAsignados()).thenReturn(asignados);

        DetalleProyectoResponse detalle = servicio.obtener(1L, usuarioGoogle);

        assertEquals(1L, detalle.proyecto().getIdProyecto());
    }

    @Test
    void operativoNoPuedeConsultarProyectoNoAsignado() {
        when(usuario.getRol()).thenReturn(RolUsuario.OPERATIVO);
        when(usuario.getProyectosAsignados()).thenReturn(new HashSet<Proyecto>());

        try {
            servicio.obtener(1L, usuarioGoogle);
            fail("Se esperaba un rechazo de acceso");
        } catch (AccessDeniedException excepcion) {
            assertNotNull(excepcion.getMessage());
        }
    }

    @Test
    void externoPuedeConsultarProyectoDeSuEmpresa() {
        when(usuario.getRol()).thenReturn(RolUsuario.EXTERNO);
        when(usuario.getIdEmpresa()).thenReturn(10L);
        DetalleProyectoResponse detalle = servicio.obtener(1L, usuarioGoogle);
        assertEquals(1L, detalle.proyecto().getIdProyecto());
    }

    @Test
    void externoNoPuedeConsultarProyectoDeOtraEmpresa() {
        when(usuario.getRol()).thenReturn(RolUsuario.EXTERNO);
        when(usuario.getIdEmpresa()).thenReturn(99L);
        try {
            servicio.obtener(1L, usuarioGoogle);
            fail("Se esperaba un rechazo de acceso");
        } catch (AccessDeniedException excepcion) {
            assertNotNull(excepcion.getMessage());
        }
    }

    @Test
    void usuarioDeshabilitadoNoPuedeConsultar() {
        when(usuario.isHabilitado()).thenReturn(false);
        try {
            servicio.obtener(1L, usuarioGoogle);
            fail("Se esperaba un rechazo de acceso");
        } catch (AccessDeniedException excepcion) {
            assertNotNull(excepcion.getMessage());
        }
    }

    @Test
    void proyectoInexistenteDevuelveError404() {
        when(proyectoRepository.findById(15L)).thenReturn(Optional.empty());
        try {
            servicio.obtener(15L, usuarioGoogle);
            fail("Se esperaba el error 404");
        } catch (ResponseStatusException excepcion) {
            assertEquals(404, excepcion.getStatusCode().value());
        }
    }
}
