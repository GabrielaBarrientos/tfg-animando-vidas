package org.tfg.backend.taller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.docente.DocenteProyecto;
import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformTallerResponse;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.seguimiento.ConfiguracionSeguimiento;
import org.tfg.backend.seguimiento.ConfiguracionSeguimientoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TallerServiceTests {
    private TallerService servicio;
    private ProyectoRepository proyectos;
    private UsuarioRepository usuarios;
    private DocenteProyectoRepository asociaciones;
    private TallerRepository talleres;
    private ClaseTallerRepository clases;
    private PlanificacionProyectoRepository planes;
    private ConfiguracionSeguimientoRepository seguimientos;
    private EducationalPlatformClient plataforma;
    private OidcUser google;
    private Usuario usuario;
    private Proyecto proyecto;

    @BeforeEach
    void preparar() {
        proyectos = mock(ProyectoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        asociaciones = mock(DocenteProyectoRepository.class);
        talleres = mock(TallerRepository.class);
        when(talleres.save(any(Taller.class))).thenAnswer(org.mockito.AdditionalAnswers.returnsFirstArg());
        clases = mock(ClaseTallerRepository.class);
        planes = mock(PlanificacionProyectoRepository.class);
        seguimientos = mock(ConfiguracionSeguimientoRepository.class);
        plataforma = mock(EducationalPlatformClient.class);
        servicio = new TallerService(proyectos, usuarios, asociaciones, talleres, clases, planes,
                seguimientos, plataforma, new TallerSeguimientoCalculador());
        google = mock(OidcUser.class);
        usuario = mock(Usuario.class);
        proyecto = mock(Proyecto.class);
        when(google.getSubject()).thenReturn("sub-test");
        when(usuarios.findByGoogleSub("sub-test")).thenReturn(Optional.of(usuario));
        when(usuario.isHabilitado()).thenReturn(true);
        when(usuario.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);
        when(proyectos.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyecto.getIdProyecto()).thenReturn(1L);
        when(proyecto.getNombre()).thenReturn("Una noche en el prado");
        when(proyecto.getAnio()).thenReturn((short) 2026);
        Empresa empresa = mock(Empresa.class);
        when(proyecto.getEmpresa()).thenReturn(empresa);
        when(empresa.getNombre()).thenReturn("Fundación Prosegur");
    }

    private void planificar() {
        PlanificacionProyecto plan = mock(PlanificacionProyecto.class);
        when(planes.findByProyecto_IdProyecto(1L)).thenReturn(Optional.of(plan));
        when(plan.getCantidadClases()).thenReturn(6);
    }

    @Test
    void sinSesionNoPuedeConsultar() {
        try {
            servicio.listar(1L, null);
            fail("Se esperaba un rechazo de acceso");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
    }

    @Test
    void listarTalleresSoloConsultaPostgresqlSinAccederALaPlataforma() {
        planificar();
        ConfiguracionSeguimiento configuracion = mock(ConfiguracionSeguimiento.class);
        when(seguimientos.findByProyecto_IdProyecto(1L)).thenReturn(Optional.of(configuracion));
        when(talleres.findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(1L))
                .thenReturn(List.of());

        assertTrue(servicio.listar(1L, google).isEmpty());
        verify(talleres).findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(1L);
        verifyNoInteractions(plataforma);
    }

    @Test
    void consultarMetadatosNoSincronizaLaPlataforma() {
        when(proyecto.getEstado()).thenReturn(org.tfg.backend.proyecto.EstadoProyecto.PLANIFICADO);
        servicio.obtenerMetadatos(1L, google);
        verifyNoInteractions(plataforma);
    }

    @Test
    void usuarioNoRegistradoImpideSincronizar() {
        planificar();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur")).thenReturn(List.of(
                new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "sin.registro", List.of())));
        try {
            servicio.sincronizar(1L, google);
            fail("Se esperaba rechazo por usuario inexistente");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
    }

    @Test
    void tallerAusenteSeMarcaPendienteSinEliminarse() {
        planificar();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur")).thenReturn(List.of());
        Taller anterior = mock(Taller.class);
        when(anterior.getIdTallerPlataforma()).thenReturn("t-antiguo");
        when(talleres.findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(1L))
                .thenReturn(List.of(anterior));
        assertEquals(0, servicio.sincronizar(1L, google));
        verify(anterior).marcarPendienteBaja();
        verify(talleres, never()).delete(any(Taller.class));
        verify(proyecto).registrarSincronizacionTalleres(0);
        verify(proyecto).actualizarEstadoPorTalleres(true);
    }

    @Test
    void validaAsociacionMedianteUsuarioPlataforma() {
        planificar();
        DocenteProyecto asociacion = mock(DocenteProyecto.class);
        org.tfg.backend.docente.Docente docente = mock(org.tfg.backend.docente.Docente.class);
        when(asociacion.getUsuarioPlataforma()).thenReturn("maria.perez");
        when(asociacion.getDocente()).thenReturn(docente);
        when(docente.getIdDocente()).thenReturn(10L);
        when(asociacion.getCantidadAlumnos()).thenReturn(25);
        when(asociaciones.findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(1L))
                .thenReturn(List.of(asociacion));
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur")).thenReturn(List.of(
                new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "maria.perez", List.of(
                        new PlatformTallerResponse.ClasePlataforma(7L, 1,
                                java.time.LocalDate.of(2026, 8, 1), 20, 1)))));
        assertEquals(1, servicio.sincronizar(1L, google));
        verify(talleres).save(any(Taller.class));
        ArgumentCaptor<ClaseTaller> claseGuardada = ArgumentCaptor.forClass(ClaseTaller.class);
        verify(clases).save(claseGuardada.capture());
        assertEquals(7L, claseGuardada.getValue().getIdClasePlataforma());
        assertEquals(1, claseGuardada.getValue().getNumeroClase());
        verify(proyecto).registrarSincronizacionTalleres(1);
        verify(proyecto).actualizarEstadoPorTalleres(true);
    }

    @Test
    void enviaLosTresCriteriosAlClienteDePlataforma() {
        planificar();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur"))
                .thenReturn(List.of());

        assertEquals(0, servicio.sincronizar(1L, google));
        verify(plataforma).obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur");
    }

    @Test
    void rechazaTallerDeOtroProyectoAntesDeModificarCopiasLocales() {
        planificar();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur"))
                .thenReturn(List.of(new PlatformTallerResponse("t-otro", "Otro proyecto",
                        (short) 2026, "Fundación Prosegur", "lara.kon", List.of())));

        try {
            servicio.sincronizar(1L, google);
            fail("La respuesta de plataforma debe corresponder al proyecto consultado");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
        verify(proyecto, never()).registrarSincronizacionTalleres(anyInt());
    }

    @Test
    void rechazaParticipantesSuperioresAlTotalDocente() {
        planificar();
        DocenteProyecto asociacion = mock(DocenteProyecto.class);
        org.tfg.backend.docente.Docente docente = mock(org.tfg.backend.docente.Docente.class);
        when(asociacion.getUsuarioPlataforma()).thenReturn("maria.perez");
        when(asociacion.getDocente()).thenReturn(docente);
        when(docente.getIdDocente()).thenReturn(10L);
        when(asociacion.getCantidadAlumnos()).thenReturn(25);
        when(asociaciones.findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(1L))
                .thenReturn(List.of(asociacion));
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur")).thenReturn(List.of(
                new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "maria.perez", List.of(
                        new PlatformTallerResponse.ClasePlataforma(7L, 1,
                                java.time.LocalDate.of(2026, 8, 1), 26, 1)))));
        try {
            servicio.sincronizar(1L, google);
            fail("Se esperaba validación de participantes");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
        verify(proyecto, never()).registrarSincronizacionTalleres(anyInt());
    }

    @Test
    void rechazaClaseSinIdExternoAntesDeGuardar() {
        planificar();
        registrarDocenteParaValidacion();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur"))
                .thenReturn(List.of(new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "maria.perez", List.of(
                        new PlatformTallerResponse.ClasePlataforma(null, 1,
                                java.time.LocalDate.of(2026, 8, 1), 20, 1)))));
        try {
            servicio.sincronizar(1L, google);
            fail("La clase debe rechazarse antes de guardar datos");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
        verify(clases, never()).save(any(ClaseTaller.class));
    }

    @Test
    void rechazaIdentificadoresDeClaseRepetidosAntesDeGuardar() {
        planificar();
        registrarDocenteParaValidacion();
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur"))
                .thenReturn(List.of(new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "maria.perez", List.of(
                        new PlatformTallerResponse.ClasePlataforma(7L, 1,
                                java.time.LocalDate.of(2026, 8, 1), 20, 1),
                        new PlatformTallerResponse.ClasePlataforma(7L, 2,
                                java.time.LocalDate.of(2026, 8, 2), 20, 1)))));
        try {
            servicio.sincronizar(1L, google);
            fail("La clase debe rechazarse antes de guardar datos");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
        verify(clases, never()).save(any(ClaseTaller.class));
    }

    @Test
    void rechazaIdExternoYaAsociadoAOtraClase() {
        planificar();
        registrarDocenteParaValidacion();
        ClaseTaller anterior = mock(ClaseTaller.class);
        Taller otroTaller = mock(Taller.class);
        when(otroTaller.getIdTallerPlataforma()).thenReturn("t-otro");
        when(anterior.getTaller()).thenReturn(otroTaller);
        when(clases.findByIdClasePlataforma(7L)).thenReturn(Optional.of(anterior));
        when(plataforma.obtenerTalleres("Una noche en el prado", (short) 2026, "Fundación Prosegur"))
                .thenReturn(List.of(new PlatformTallerResponse("t1", "Una noche en el prado", (short) 2026,
                        "Fundación Prosegur", "maria.perez", List.of(
                        new PlatformTallerResponse.ClasePlataforma(7L, 1,
                                java.time.LocalDate.of(2026, 8, 1), 20, 1)))));
        try {
            servicio.sincronizar(1L, google);
            fail("No se puede asociar un identificador a otra clase");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verify(talleres, never()).save(any(Taller.class));
        verify(clases, never()).save(any(ClaseTaller.class));
    }

    private void registrarDocenteParaValidacion() {
        DocenteProyecto asociacion = mock(DocenteProyecto.class);
        org.tfg.backend.docente.Docente docente = mock(org.tfg.backend.docente.Docente.class);
        when(asociacion.getUsuarioPlataforma()).thenReturn("maria.perez");
        when(asociacion.getDocente()).thenReturn(docente);
        when(docente.getIdDocente()).thenReturn(10L);
        when(asociacion.getCantidadAlumnos()).thenReturn(25);
        when(asociaciones.findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(1L))
                .thenReturn(List.of(asociacion));
    }

    @Test
    void metadatosSeConsultanConLosPermisosDelProyecto() {
        when(proyecto.getEstado()).thenReturn(org.tfg.backend.proyecto.EstadoProyecto.EN_CURSO);
        when(proyecto.getCantidadTalleresPlataforma()).thenReturn(3);
        java.time.OffsetDateTime fecha = java.time.OffsetDateTime.parse("2026-10-20T20:55:13Z");
        when(proyecto.getUltimaSincronizacionTalleres()).thenReturn(fecha);
        TalleresMetadatosResponse respuesta = servicio.obtenerMetadatos(1L, google);
        assertEquals("EN_CURSO", respuesta.estadoProyecto());
        assertEquals(3, respuesta.cantidadTalleresPlataforma());
        assertEquals(fecha, respuesta.ultimaSincronizacion());
    }

    @Test
    void eliminarUltimoTallerDevuelveElProyectoAPlanificado() {
        Taller anterior = mock(Taller.class);
        DocenteProyecto asignacion = mock(DocenteProyecto.class);
        when(talleres.findByIdTallerPlataforma("t1")).thenReturn(Optional.of(anterior));
        when(anterior.getAsignacion()).thenReturn(asignacion);
        when(asignacion.getProyecto()).thenReturn(proyecto);
        when(anterior.isPendienteBaja()).thenReturn(true);
        when(talleres.existsByAsignacion_Proyecto_IdProyecto(1L)).thenReturn(false);
        servicio.eliminarCopiaLocal(1L, "t1", google);
        verify(talleres).delete(anterior);
        verify(proyecto).actualizarEstadoPorTalleres(false);
    }

    @Test
    void noPermiteEliminarCopiaSinBajaPendiente() {
        Taller anterior = mock(Taller.class);
        DocenteProyecto asignacion = mock(DocenteProyecto.class);
        when(talleres.findByIdTallerPlataforma("t1")).thenReturn(Optional.of(anterior));
        when(anterior.getAsignacion()).thenReturn(asignacion);
        when(asignacion.getProyecto()).thenReturn(proyecto);
        when(anterior.isPendienteBaja()).thenReturn(false);
        try {
            servicio.eliminarCopiaLocal(1L, "t1", google);
            fail("La copia local no está pendiente de baja");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verify(talleres, never()).delete(any(Taller.class));
    }
}
