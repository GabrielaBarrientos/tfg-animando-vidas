package org.tfg.backend.evidencia;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenciasZipServiceTests {
    private TallerService talleres;
    private ClaseTallerRepository clases;
    private EvidenciaRepository evidencias;
    private EducationalPlatformClient plataforma;
    private EvidenciasZipService servicio;
    private Taller taller;
    private ClaseTaller clase;
    private OidcUser usuario;

    @BeforeEach
    void preparar() {
        talleres = mock(TallerService.class);
        clases = mock(ClaseTallerRepository.class);
        evidencias = mock(EvidenciaRepository.class);
        plataforma = mock(EducationalPlatformClient.class);
        servicio = new EvidenciasZipService(talleres, clases, evidencias, plataforma);
        taller = mock(Taller.class);
        clase = mock(ClaseTaller.class);
        usuario = mock(OidcUser.class);
        when(taller.getIdTaller()).thenReturn(15L);
        when(clase.getIdClaseTaller()).thenReturn(100L);
        when(clase.getTaller()).thenReturn(taller);
        when(talleres.buscarTallerAutorizado(20L, "8", usuario)).thenReturn(taller);
    }

    @Test
    void zipIncluyeSoloSeleccionadasSinDuplicar() throws Exception {
        when(clases.findById(100L)).thenReturn(Optional.of(clase));
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        Evidencia util = new Evidencia(clase, 1L, "uno.jpg", "image/jpeg", 3L, ahora);
        util.marcar(true, true);
        Evidencia normal = new Evidencia(clase, 2L, "dos.jpg", "image/jpeg", 2L, ahora);
        Evidencia destacada = new Evidencia(clase, 3L, "tres.jpg", "image/jpeg", 4L, ahora);
        destacada.marcar(false, true);
        when(evidencias.findByClase_IdClaseTallerOrderByIdEvidenciaAsc(100L))
                .thenReturn(List.of(util, normal, destacada));
        when(plataforma.abrirArchivo(1L, null)).thenReturn(new EducationalPlatformClient.ArchivoRemoto(
                200, new ByteArrayInputStream(new byte[]{1,2,3}), "image/jpeg", 3, null));
        when(plataforma.abrirArchivo(3L, null)).thenReturn(new EducationalPlatformClient.ArchivoRemoto(
                200, new ByteArrayInputStream(new byte[]{1,2,3,4}), "image/jpeg", 4, null));
        Path archivo = servicio.generarZip(20L, "8", 100L, usuario);
        int cantidad = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivo))) {
            while (zip.getNextEntry() != null) {
                cantidad++;
                zip.closeEntry();
            }
        } finally {
            Files.deleteIfExists(archivo);
        }
        assertEquals(2, cantidad);
        verify(plataforma, never()).abrirArchivo(2L, null);
    }

    @Test
    void sinValoracionesNoSeGeneraZip() {
        when(clases.findById(100L)).thenReturn(Optional.of(clase));
        when(evidencias.findByClase_IdClaseTallerOrderByIdEvidenciaAsc(100L))
                .thenReturn(List.of(new Evidencia(clase, 1L, "foto.jpg", "image/jpeg", 5L,
                        OffsetDateTime.now(ZoneOffset.UTC))));
        try {
            servicio.generarZip(20L, "8", 100L, usuario);
            fail("Se esperaba 409");
        } catch (ResponseStatusException error) {
            assertEquals(409, error.getStatusCode().value());
        }
        verifyNoInteractions(plataforma);
    }
    @Test
    void binarioIncompletoRechazaElZip() {
        when(clases.findById(100L)).thenReturn(Optional.of(clase));
        Evidencia util = new Evidencia(clase, 1L, "incompleto.jpg", "image/jpeg", 5L,
                OffsetDateTime.now(ZoneOffset.UTC));
        util.marcar(true, false);
        when(evidencias.findByClase_IdClaseTallerOrderByIdEvidenciaAsc(100L))
                .thenReturn(List.of(util));
        when(plataforma.abrirArchivo(1L, null)).thenReturn(new EducationalPlatformClient.ArchivoRemoto(
                200, new ByteArrayInputStream(new byte[]{1, 2}), "image/jpeg", 2, null));

        try {
            servicio.generarZip(20L, "8", 100L, usuario);
            fail("Se esperaba un error por archivo incompleto");
        } catch (ResponseStatusException error) {
            assertEquals(502, error.getStatusCode().value());
        }
    }

    @Test
    void nombresRepetidosSeDiferencianDentroDelZip() throws Exception {
        when(clases.findById(100L)).thenReturn(Optional.of(clase));
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        Evidencia primera = new Evidencia(clase, 1L, "foto.jpg", "image/jpeg", 1L, ahora);
        primera.marcar(true, false);
        Evidencia segunda = new Evidencia(clase, 2L, "foto.jpg", "image/jpeg", 1L, ahora);
        segunda.marcar(false, true);
        when(evidencias.findByClase_IdClaseTallerOrderByIdEvidenciaAsc(100L))
                .thenReturn(List.of(primera, segunda));
        when(plataforma.abrirArchivo(1L, null)).thenReturn(new EducationalPlatformClient.ArchivoRemoto(
                200, new ByteArrayInputStream(new byte[]{1}), "image/jpeg", 1, null));
        when(plataforma.abrirArchivo(2L, null)).thenReturn(new EducationalPlatformClient.ArchivoRemoto(
                200, new ByteArrayInputStream(new byte[]{2}), "image/jpeg", 1, null));

        Path archivo = servicio.generarZip(20L, "8", 100L, usuario);
        java.util.Set<String> nombres = new java.util.HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivo))) {
            java.util.zip.ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                nombres.add(entrada.getName());
                zip.closeEntry();
            }
        } finally {
            Files.deleteIfExists(archivo);
        }
        assertEquals(2, nombres.size());
        assertTrue(nombres.contains("foto.jpg"));
    }
}
