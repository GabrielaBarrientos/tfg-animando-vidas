package org.tfg.backend.taller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.docente.Docente;
import org.tfg.backend.docente.DocenteProyecto;
import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformDetalleTallerResponse;
import org.tfg.backend.planificacion.FrecuenciaClases;
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

class DetalleTallerServiceTests {
    private TallerService servicio;
    private ProyectoRepository proyectos;
    private UsuarioRepository usuarios;
    private TallerRepository talleres;
    private ClaseTallerRepository clases;
    private PlanificacionProyectoRepository planificaciones;
    private ConfiguracionSeguimientoRepository seguimientos;
    private EducationalPlatformClient plataforma;
    private Proyecto proyecto;
    private Taller taller;
    private OidcUser google;

    @BeforeEach
    void preparar() {
        proyectos = mock(ProyectoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        talleres = mock(TallerRepository.class);
        clases = mock(ClaseTallerRepository.class);
        planificaciones = mock(PlanificacionProyectoRepository.class);
        seguimientos = mock(ConfiguracionSeguimientoRepository.class);
        plataforma = mock(EducationalPlatformClient.class);
        servicio = new TallerService(proyectos, usuarios, mock(DocenteProyectoRepository.class),
                talleres, clases, planificaciones, seguimientos, plataforma,
                new TallerSeguimientoCalculador());

        proyecto = mock(Proyecto.class);
        when(proyectos.findById(20L)).thenReturn(Optional.of(proyecto));
        when(proyecto.getIdProyecto()).thenReturn(20L);
        google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn("sub-taller");
        Usuario usuario = mock(Usuario.class);
        when(usuarios.findByGoogleSub("sub-taller")).thenReturn(Optional.of(usuario));
        when(usuario.isHabilitado()).thenReturn(true);
        when(usuario.getRol()).thenReturn(RolUsuario.ADMINISTRADOR);

        Docente docente = mock(Docente.class);
        when(docente.getNombres()).thenReturn("Lara");
        when(docente.getApellidos()).thenReturn("Kon");
        DocenteProyecto asignacion = mock(DocenteProyecto.class);
        when(asignacion.getProyecto()).thenReturn(proyecto);
        when(asignacion.getDocente()).thenReturn(docente);
        when(asignacion.getUsuarioPlataforma()).thenReturn("lara.kon");
        when(asignacion.getPais()).thenReturn("Argentina");
        when(asignacion.getProvincia()).thenReturn("Buenos Aires");
        when(asignacion.getCentroEducativo()).thenReturn("Escuela 15");
        when(asignacion.getCantidadAlumnos()).thenReturn(25);
        taller = mock(Taller.class);
        when(taller.getAsignacion()).thenReturn(asignacion);
        when(taller.getIdTallerPlataforma()).thenReturn("t-20");
        when(talleres.findById("t-20")).thenReturn(Optional.of(taller));

        PlanificacionProyecto planificacion = mock(PlanificacionProyecto.class);
        when(planificacion.getFechaInicio()).thenReturn(LocalDate.of(2026, 7, 1));
        when(planificacion.getCantidadClases()).thenReturn(4);
        when(planificacion.getFrecuenciaClases()).thenReturn(FrecuenciaClases.MENSUAL);
        when(planificaciones.findByProyecto_IdProyecto(20L)).thenReturn(Optional.of(planificacion));
        ConfiguracionSeguimiento seguimiento = mock(ConfiguracionSeguimiento.class);
        when(seguimiento.getUmbralProximoAtrasoSemanas()).thenReturn(2);
        when(seguimiento.getUmbralAtrasoSemanas()).thenReturn(3);
        when(seguimientos.findByProyecto_IdProyecto(20L)).thenReturn(Optional.of(seguimiento));
    }

    @Test
    void consultaCuatroClasesPrevistasConDosRealizadasSinLlamarALaPlataforma() {
        ClaseTaller primera = mock(ClaseTaller.class);
        when(primera.getNumeroClase()).thenReturn(1);
        when(primera.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 7, 12));
        when(primera.getCantidadParticipantes()).thenReturn(20);
        when(primera.getCantidadEvidencias()).thenReturn(2);
        ClaseTaller segunda = mock(ClaseTaller.class);
        when(segunda.getNumeroClase()).thenReturn(2);
        when(segunda.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 8, 15));
        when(segunda.getCantidadParticipantes()).thenReturn(17);
        when(segunda.getCantidadEvidencias()).thenReturn(0);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("t-20"))
                .thenReturn(List.of(primera, segunda));

        DetalleTallerResponse detalle = servicio.obtenerDetalle(20L, "t-20", google);

        assertEquals("Lara Kon", detalle.nombreDocente());
        assertEquals("lara.kon", detalle.usuarioPlataforma());
        assertEquals(4, detalle.totalClases());
        assertEquals(4, detalle.clases().size());
        assertEquals(2, detalle.avanceReal());
        assertTrue(detalle.diferencia() >= 0);
        assertEquals(TallerResumenResponse.EstadoEvidencias.PENDIENTES, detalle.evidencias());
        assertEquals(LocalDate.of(2026, 7, 31), detalle.clases().get(0).fechaPrevista());
        assertEquals(Integer.valueOf(20), detalle.clases().get(0).cantidadParticipantes());
        assertEquals(2, detalle.clases().get(0).cantidadEvidencias());
        assertEquals(DetalleTallerResponse.EstadoClase.REALIZADA, detalle.clases().get(0).estado());
        assertEquals(LocalDate.of(2026, 9, 29), detalle.clases().get(2).fechaPrevista());
        assertNull(detalle.clases().get(2).fechaRealizacion());
        assertNull(detalle.clases().get(2).cantidadParticipantes());
        assertEquals(DetalleTallerResponse.EstadoClase.PENDIENTE, detalle.clases().get(2).estado());
        verifyNoInteractions(plataforma);
    }

    @Test
    void noPermiteConsultarTallerDeOtroProyecto() {
        Proyecto otroProyecto = mock(Proyecto.class);
        when(otroProyecto.getIdProyecto()).thenReturn(999L);
        when(taller.getAsignacion().getProyecto()).thenReturn(otroProyecto);
        try {
            servicio.obtenerDetalle(20L, "t-20", google);
            fail("Se esperaba 404 para un taller de otro proyecto");
        } catch (ResponseStatusException excepcion) {
            assertEquals(404, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(plataforma);
        verifyNoInteractions(clases);
    }

    @Test
    void noPermiteConsultarSinSesion() {
        try {
            servicio.obtenerDetalle(20L, "t-20", null);
            fail("Se esperaba rechazo de acceso");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
        verifyNoInteractions(plataforma);
    }

    @Test
    void informaTallerInexistente() {
        try {
            servicio.obtenerDetalle(20L, "inexistente", google);
            fail("Se esperaba 404 para taller inexistente");
        } catch (ResponseStatusException excepcion) {
            assertEquals(404, excepcion.getStatusCode().value());
        }
    }

    @Test
    void sincronizarClasesConsultaUnicamenteElTallerSeleccionado() {
        when(talleres.findById("8")).thenReturn(Optional.of(taller));
        when(taller.getIdTallerPlataforma()).thenReturn("8");
        when(plataforma.obtenerDetalleTaller(8)).thenReturn(new PlatformDetalleTallerResponse(8,
                List.of(
                        new PlatformDetalleTallerResponse.ClaseDetallePlataforma(1,
                                "ENCUENTRO 1", "INTRODUCCIÓN AL DIBUJO", "Realizada",
                                LocalDate.of(2026, 5, 15), 9, 0),
                        new PlatformDetalleTallerResponse.ClaseDetallePlataforma(2,
                                "ENCUENTRO 2", "INTRODUCCIÓN AL DIBUJO", "Pendiente",
                                null, null, null))));
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of());

        int cantidad = servicio.sincronizarClases(20L, "8", google);

        assertEquals(2, cantidad);
        verify(plataforma).obtenerDetalleTaller(8);
        verify(plataforma, never()).obtenerTalleres(anyString(), anyShort(), anyString());
        verify(clases, times(2)).save(any(ClaseTaller.class));
        verify(taller).registrarSincronizacionClases();
        verify(proyecto, never()).registrarSincronizacionTalleres(anyInt());
    }

    @Test
    void rechazaUnDetalleQueCorrespondeAOtroTaller() {
        when(talleres.findById("8")).thenReturn(Optional.of(taller));
        when(plataforma.obtenerDetalleTaller(8))
                .thenReturn(new PlatformDetalleTallerResponse(9, List.of()));
        try {
            servicio.sincronizarClases(20L, "8", google);
            fail("Se esperaba rechazar la respuesta de otro taller");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(clases);
        verify(taller, never()).registrarSincronizacionClases();
    }

    @Test
    void claseInvalidaNoModificaLosDatosLocales() {
        when(talleres.findById("8")).thenReturn(Optional.of(taller));
        when(plataforma.obtenerDetalleTaller(8)).thenReturn(new PlatformDetalleTallerResponse(8,
                List.of(new PlatformDetalleTallerResponse.ClaseDetallePlataforma(
                        1, "ENCUENTRO 1", "MÓDULO", "Pendiente",
                        LocalDate.of(2026, 5, 15), 9, 0))));
        try {
            servicio.sincronizarClases(20L, "8", google);
            fail("Se esperaba rechazar una clase pendiente con fecha");
        } catch (ResponseStatusException excepcion) {
            assertEquals(502, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(clases);
        verify(taller, never()).registrarSincronizacionClases();
    }

    @Test
    void soloPermiteSincronizarClasesDeTalleresDelProyecto() {
        Proyecto otro = mock(Proyecto.class);
        when(otro.getIdProyecto()).thenReturn(999L);
        when(talleres.findById("8")).thenReturn(Optional.of(taller));
        when(taller.getAsignacion().getProyecto()).thenReturn(otro);
        try {
            servicio.sincronizarClases(20L, "8", google);
            fail("Se esperaba impedir la sincronización de otro proyecto");
        } catch (ResponseStatusException excepcion) {
            assertEquals(404, excepcion.getStatusCode().value());
        }
        verifyNoInteractions(plataforma);
    }

    @Test
    void detalleIncluyeNombreDeClaseModuloYFechaDeSincronizacion() {
        ClaseTaller clase = mock(ClaseTaller.class);
        when(clase.getNumeroClase()).thenReturn(1);
        when(clase.getNombreClase()).thenReturn("ENCUENTRO 1");
        when(clase.getNombreModulo()).thenReturn("INTRODUCCIÓN AL DIBUJO");
        when(clase.getFechaRealizacion()).thenReturn(LocalDate.of(2026, 7, 12));
        when(clase.getCantidadParticipantes()).thenReturn(9);
        when(clase.getCantidadEvidencias()).thenReturn(0);
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("t-20"))
                .thenReturn(List.of(clase));
        java.time.OffsetDateTime fecha = java.time.OffsetDateTime.parse("2026-11-04T19:30:00Z");
        when(taller.getUltimaSincronizacionClases()).thenReturn(fecha);
        DetalleTallerResponse respuesta = servicio.obtenerDetalle(20L, "t-20", google);
        assertEquals("ENCUENTRO 1", respuesta.clases().get(0).nombreClase());
        assertEquals("INTRODUCCIÓN AL DIBUJO", respuesta.clases().get(0).nombreModulo());
        assertEquals(fecha, respuesta.ultimaSincronizacion());
        verifyNoInteractions(plataforma);
    }

}
