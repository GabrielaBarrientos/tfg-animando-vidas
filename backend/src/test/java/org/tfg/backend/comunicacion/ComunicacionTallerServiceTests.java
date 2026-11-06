package org.tfg.backend.comunicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioAutorizacionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComunicacionTallerServiceTests {
    private final TallerService talleres = mock(TallerService.class);
    private final UsuarioAutorizacionService usuarios = mock(UsuarioAutorizacionService.class);
    private final ComunicacionTallerRepository repositorio = mock(ComunicacionTallerRepository.class);
    private final ComunicacionTallerService servicio = new ComunicacionTallerService(talleres, usuarios, repositorio);
    private final OidcUser principal = mock(OidcUser.class);
    private final Usuario usuario = mock(Usuario.class);
    private final Taller taller = mock(Taller.class);
    private final LocalDate contacto = LocalDate.of(2026, 5, 10);

    @BeforeEach
    void preparar() {
        when(principal.getSubject()).thenReturn("google-test");
        when(usuarios.buscarUsuarioAutorizado("google-test")).thenReturn(Optional.of(usuario));
        when(usuario.getRol()).thenReturn(RolUsuario.OPERATIVO);
        when(usuario.getNombres()).thenReturn("Ana");
        when(usuario.getApellidos()).thenReturn("López");
        when(talleres.buscarTallerAutorizado(2040L, "8", principal)).thenReturn(taller);
        when(taller.getIdTaller()).thenReturn(77L);
    }

    private CrearComunicacionRequest nueva() {
        return new CrearComunicacionRequest(contacto, CanalComunicacion.WHATSAPP, null,
                MotivoComunicacion.ATRASO_EN_EL_AVANCE, null, AccionComunicacion.RECORDATORIO,
                ResultadoComunicacion.ESPERANDO_RESPUESTA, null, "Se enviaron recordatorios.");
    }

    private void esperar400Crear(CrearComunicacionRequest datos) {
        try {
            servicio.crear(2040L, "8", principal, datos);
            fail("Se esperaba código 400");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void registraContactoConUsuarioInternoYDatosInmutables() {
        ComunicacionResponse response = servicio.crear(2040L, "8", principal, nueva());
        org.mockito.ArgumentCaptor<ComunicacionTaller> captor =
                org.mockito.ArgumentCaptor.forClass(ComunicacionTaller.class);
        verify(repositorio).saveAndFlush(captor.capture());
        ComunicacionTaller creada = captor.getValue();
        assertSame(taller, creada.getTaller());
        assertSame(usuario, creada.getUsuarioRegistro());
        assertEquals(contacto, creada.getFechaContacto());
        assertEquals(AccionComunicacion.RECORDATORIO, creada.getAccionRealizada());
        assertEquals("Se enviaron recordatorios.", response.observaciones());
        assertEquals("Ana López", response.registradoPor());
    }

    @Test
    void consultaSoloComunicacionesDelTallerAutorizadoSinPlataforma() {
        ComunicacionTaller creada = new ComunicacionTaller(taller, usuario, nueva(), null, null,
                "Se enviaron recordatorios.");
        when(repositorio.findByTaller_IdTallerOrderByFechaContactoDescIdComunicacionDesc(77L))
                .thenReturn(List.of(creada));
        List<ComunicacionResponse> resultado = servicio.listar(2040L, "8", principal);
        assertEquals(1, resultado.size());
        verify(repositorio).findByTaller_IdTallerOrderByFechaContactoDescIdComunicacionDesc(77L);
    }

    @Test
    void usuarioExternoNoConsultaNiRegistra() {
        when(usuario.getRol()).thenReturn(RolUsuario.EXTERNO);
        try {
            servicio.listar(2040L, "8", principal);
            fail("Los externos no pueden consultar comunicaciones");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
        try {
            servicio.crear(2040L, "8", principal, nueva());
            fail("Los externos no pueden registrar comunicaciones");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
        verifyNoInteractions(repositorio);
        verify(talleres, never()).buscarTallerAutorizado(any(), any(), any());
    }

    @Test
    void noPermiteUsuariosDeshabilitados() {
        when(usuarios.buscarUsuarioAutorizado("google-test")).thenReturn(Optional.empty());
        try {
            servicio.listar(2040L, "8", principal);
            fail("Se esperaba rechazo de usuario deshabilitado");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
        verifyNoInteractions(repositorio);
    }

    @Test
    void registraAccionConsultaConMotivoIndependiente() {
        CrearComunicacionRequest datos = new CrearComunicacionRequest(contacto,
                CanalComunicacion.WHATSAPP, null, MotivoComunicacion.ATRASO_EN_EL_AVANCE,
                null, AccionComunicacion.CONSULTA, ResultadoComunicacion.ESPERANDO_RESPUESTA,
                null, "Consulta realizada al docente.");
        servicio.crear(2040L, "8", principal, datos);
        org.mockito.ArgumentCaptor<ComunicacionTaller> captor =
                org.mockito.ArgumentCaptor.forClass(ComunicacionTaller.class);
        verify(repositorio).saveAndFlush(captor.capture());
        assertEquals(AccionComunicacion.CONSULTA, captor.getValue().getAccionRealizada());
        assertEquals(MotivoComunicacion.ATRASO_EN_EL_AVANCE, captor.getValue().getMotivo());
    }

    @Test
    void permiteCanalYMotivoOtroConVeinteCaracteres() {
        CrearComunicacionRequest datos = new CrearComunicacionRequest(contacto, CanalComunicacion.OTRO,
                "Visita presencial", MotivoComunicacion.OTRO, "Consulta especial",
                AccionComunicacion.SEGUIMIENTO, ResultadoComunicacion.ESPERANDO_RESPUESTA,
                null, null);
        servicio.crear(2040L, "8", principal, datos);
        org.mockito.ArgumentCaptor<ComunicacionTaller> captor =
                org.mockito.ArgumentCaptor.forClass(ComunicacionTaller.class);
        verify(repositorio).saveAndFlush(captor.capture());
        assertEquals("Visita presencial", captor.getValue().getCanalOtro());
        assertEquals("Consulta especial", captor.getValue().getMotivoOtro());
    }

    @Test
    void rechazaOtroSinTextoYTenerTextoSinSeleccionarOtro() {
        esperar400Crear(new CrearComunicacionRequest(contacto, CanalComunicacion.OTRO,
                "  ", MotivoComunicacion.CONSULTA, null, AccionComunicacion.RECLAMO,
                ResultadoComunicacion.ESPERANDO_RESPUESTA, null, null));
    }

    @Test
    void rechazaTextoOtroMayorAVeinteCaracteres() {
        esperar400Crear(new CrearComunicacionRequest(contacto, CanalComunicacion.WHATSAPP,
                null, MotivoComunicacion.OTRO, "123456789012345678901",
                AccionComunicacion.RECORDATORIO,
                ResultadoComunicacion.ESPERANDO_RESPUESTA, null, null));
    }

    @Test
    void noPermiteFechaContactoFutura() {
        esperar400Crear(new CrearComunicacionRequest(LocalDate.now().plusDays(1),
                CanalComunicacion.WHATSAPP, null, MotivoComunicacion.CONSULTA, null,
                AccionComunicacion.SEGUIMIENTO, ResultadoComunicacion.ESPERANDO_RESPUESTA,
                null, null));
    }

    @Test
    void fechaCompromisoNoPuedeSerAnteriorAlContacto() {
        esperar400Crear(new CrearComunicacionRequest(contacto, CanalComunicacion.CORREO,
                null, MotivoComunicacion.CONSULTA, null,
                AccionComunicacion.SEGUIMIENTO, ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION,
                contacto.minusDays(1), null));
    }

    @Test
    void fechaCompromisoEsOpcional() {
        servicio.crear(2040L, "8", principal,
                new CrearComunicacionRequest(contacto, CanalComunicacion.CORREO,
                        null, MotivoComunicacion.CONSULTA, null,
                        AccionComunicacion.SEGUIMIENTO,
                        ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION, null, null));
        verify(repositorio).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void actualizaSoloSeguimientoYSuFecha() {
        ComunicacionTaller existente = new ComunicacionTaller(taller, usuario, nueva(), null, null,
                "Se enviaron recordatorios.");
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.of(existente));
        ActualizarSeguimientoRequest cambio = new ActualizarSeguimientoRequest(
                ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION, contacto.plusDays(2),
                "Se comprometió a completar las clases.", contacto.plusDays(5));
        ComunicacionResponse resultado = servicio.actualizarSeguimiento(2040L, "8", 55L, principal, cambio);
        assertEquals(ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION, resultado.resultado());
        assertEquals(contacto.plusDays(5), resultado.fechaCompromiso());
        assertEquals(contacto, existente.getFechaContacto());
        assertEquals(AccionComunicacion.RECORDATORIO, existente.getAccionRealizada());
        assertEquals("Se enviaron recordatorios.", existente.getObservaciones());
        verify(repositorio).saveAndFlush(existente);
    }

    @Test
    void rechazaComunicacionDeOtroTaller() {
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.empty());
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.ESPERANDO_RESPUESTA,
                            null, null, null));
            fail("Se esperaba 404");
        } catch (ResponseStatusException error) {
            assertEquals(404, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void rechazaRespuestaSinFechaOFechaAnteriorAlContacto() {
        ComunicacionTaller existente = new ComunicacionTaller(taller, usuario, nueva(), null, null, null);
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.of(existente));
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.RESPUESTA_SIN_COMPROMISO,
                            null, "Respondió", null));
            fail("Se esperaba 400");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.RESPUESTA_SIN_COMPROMISO,
                            contacto.minusDays(1), "Respondió", null));
            fail("Se esperaba 400");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void rechazaRespuestaCuandoResultadoEsEsperando() {
        ComunicacionTaller existente = new ComunicacionTaller(taller, usuario, nueva(), null, null, null);
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.of(existente));
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.ESPERANDO_RESPUESTA,
                            contacto, "Se recibió respuesta", null));
            fail("Se esperaba 400");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void respetaLaAsignacionDelProyectoAntesDeConsultarDatos() {
        when(talleres.buscarTallerAutorizado(2040L, "8", principal))
                .thenThrow(new AccessDeniedException("No tenés acceso a este proyecto"));
        try {
            servicio.listar(2040L, "8", principal);
            fail("No se deben revelar comunicaciones de proyectos no asignados");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaRespuestaSiNoSePudoContactar() {
        ComunicacionTaller existente = new ComunicacionTaller(taller, usuario, nueva(), null, null, null);
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.of(existente));
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.NO_SE_PUDO_CONTACTAR,
                            contacto, "Se recibió respuesta", null));
            fail("El resultado no se pudo contactar no permite una respuesta");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }

    @Test
    void noPermiteCompromisoAnteriorALaRespuesta() {
        ComunicacionTaller existente = new ComunicacionTaller(taller, usuario, nueva(), null, null, null);
        when(repositorio.findByIdComunicacionAndTaller_IdTaller(55L, 77L))
                .thenReturn(Optional.of(existente));
        try {
            servicio.actualizarSeguimiento(2040L, "8", 55L, principal,
                    new ActualizarSeguimientoRequest(ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION,
                            contacto.plusDays(5), "Se comprometió", contacto.plusDays(3)));
            fail("La fecha de compromiso no debe ser anterior a la respuesta");
        } catch (ResponseStatusException error) {
            assertEquals(400, error.getStatusCode().value());
        }
        verify(repositorio, never()).saveAndFlush(any(ComunicacionTaller.class));
    }
}
