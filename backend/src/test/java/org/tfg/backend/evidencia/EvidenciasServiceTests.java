package org.tfg.backend.evidencia;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformEvidenciasResponse;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenciasServiceTests {
    private TallerService talleres;
    private ClaseTallerRepository clases;
    private EvidenciaRepository evidencias;
    private EducationalPlatformClient plataforma;
    private EvidenciasService servicio;
    private Taller taller;
    private ClaseTaller clase;
    private OidcUser usuario;

    @BeforeEach
    void preparar() {
        talleres = mock(TallerService.class);
        clases = mock(ClaseTallerRepository.class);
        evidencias = mock(EvidenciaRepository.class);
        plataforma = mock(EducationalPlatformClient.class);
        servicio = new EvidenciasService(talleres, clases, evidencias, plataforma);
        taller = mock(Taller.class);
        clase = mock(ClaseTaller.class);
        usuario = mock(OidcUser.class);
        when(taller.getIdTaller()).thenReturn(15L);
        when(taller.getIdTallerPlataforma()).thenReturn("8");
        when(clase.getIdClaseTaller()).thenReturn(100L);
        when(clase.getIdClasePlataforma()).thenReturn(8L);
        when(clase.getNumeroClase()).thenReturn(2);
        when(clase.getTaller()).thenReturn(taller);
        when(talleres.buscarTallerAutorizado(20L, "8", usuario)).thenReturn(taller);
    }

    @Test
    void sincronizaAsociandoArchivoConClaveInterna() {
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        when(plataforma.obtenerEvidencias(8)).thenReturn(new PlatformEvidenciasResponse(8,
                List.of(new PlatformEvidenciasResponse.ArchivoPlataforma(
                        1L, 8L, "evidencia j.opazo.jpg", "image/jpeg", 145382L))));
        assertEquals(1, servicio.sincronizar(20L, "8", usuario));
        org.mockito.ArgumentCaptor<Evidencia> captor = org.mockito.ArgumentCaptor.forClass(Evidencia.class);
        verify(evidencias).save(captor.capture());
        Evidencia guardada = captor.getValue();
        assertSame(clase, guardada.getClase());
        assertEquals(1L, guardada.getIdArchivoPlataforma());
        assertFalse(guardada.isEsUtil());
        assertFalse(guardada.isEsDestacada());
        assertEquals(guardada.getFechaPrimeraSincronizacion(), guardada.getFechaUltimaSincronizacion());
        verify(taller).registrarSincronizacionEvidencias(any(OffsetDateTime.class));
    }

    @Test
    void noModificaNadaSiLaClaseExternaNoExiste() {
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        when(plataforma.obtenerEvidencias(8)).thenReturn(new PlatformEvidenciasResponse(8,
                List.of(new PlatformEvidenciasResponse.ArchivoPlataforma(
                        1L, 99L, "foto.jpg", "image/jpeg", 20L))));
        try {
            servicio.sincronizar(20L, "8", usuario);
            fail("Se esperaba 502");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
        verify(evidencias, never()).save(any(Evidencia.class));
        verify(taller, never()).registrarSincronizacionEvidencias(any(OffsetDateTime.class));
    }

    @Test
    void actualizarMetadatosConservaMarcadoresYPrimeraFecha() {
        when(clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc("8"))
                .thenReturn(List.of(clase));
        when(plataforma.obtenerEvidencias(8)).thenReturn(new PlatformEvidenciasResponse(8,
                List.of(new PlatformEvidenciasResponse.ArchivoPlataforma(
                        1L, 8L, "foto nueva.jpg", "image/jpeg", 50L))));
        OffsetDateTime inicial = OffsetDateTime.of(2026, 11, 4, 10, 0, 0, 0, ZoneOffset.UTC);
        Evidencia existente = new Evidencia(clase, 1L, "vieja.jpg", "image/jpeg", 10L, inicial);
        existente.marcar(true, true);
        when(evidencias.findByIdArchivoPlataforma(1L)).thenReturn(Optional.of(existente));
        servicio.sincronizar(20L, "8", usuario);
        assertTrue(existente.isEsUtil());
        assertTrue(existente.isEsDestacada());
        assertEquals(inicial, existente.getFechaPrimeraSincronizacion());
        assertEquals("foto nueva.jpg", existente.getNombreFisico());
        verify(evidencias).save(existente);
    }

}
