package org.tfg.backend.evidencia;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

/**
 * Selecciona las evidencias valoradas y prepara un ZIP completo antes de entregarlo.
 * No modifica marcadores ni fechas de sincronización.
 */
@Service
public class EvidenciasZipService {
    @Value("${evidencias.max-zip-bytes:524288000}")
    private long maxZipBytes = 524288000L;

    private final TallerService talleres;
    private final ClaseTallerRepository clases;
    private final EvidenciaRepository evidencias;
    private final EducationalPlatformClient plataforma;

    public EvidenciasZipService(TallerService talleres, ClaseTallerRepository clases,
            EvidenciaRepository evidencias, EducationalPlatformClient plataforma) {
        this.talleres = talleres;
        this.clases = clases;
        this.evidencias = evidencias;
        this.plataforma = plataforma;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public Path generarZip(Long idProyecto, String idTaller, Long idClaseTaller, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        validarPertenenciaClase(taller, idClaseTaller);
        List<Evidencia> seleccionadas = seleccionarEvidenciasValoradas(idClaseTaller);
        return prepararArchivoZip(seleccionadas);
    }

    private void validarPertenenciaClase(Taller taller, Long idClaseTaller) {
        Optional<ClaseTaller> encontrada = clases.findById(idClaseTaller);
        if (encontrada.isEmpty()
                || !taller.getIdTaller().equals(encontrada.get().getTaller().getIdTaller())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada en el taller");
        }
    }

    private List<Evidencia> seleccionarEvidenciasValoradas(Long idClaseTaller) {
        List<Evidencia> archivos = evidencias.findByClase_IdClaseTallerOrderByIdEvidenciaAsc(idClaseTaller);
        List<Evidencia> seleccionadas = new ArrayList<>();
        long estimado = 0;

        for (Evidencia evidencia : archivos) {
            if (evidencia.isEsUtil() || evidencia.isEsDestacada()) {
                long tamanio = evidencia.getTamanioBytes();
                if (tamanio < 0 || tamanio > maxZipBytes - estimado) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "ZIP supera el límite permitido");
                }
                seleccionadas.add(evidencia);
                estimado += tamanio;
            }
        }

        if (seleccionadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay evidencias útiles o destacadas");
        }
        return seleccionadas;
    }

    private Path prepararArchivoZip(List<Evidencia> seleccionadas) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("evidencias-clase-", ".zip");
            try (OutputStream salida = Files.newOutputStream(temporal);
                    ZipOutputStream zip = new ZipOutputStream(salida)) {
                Set<String> nombresUsados = new HashSet<>();
                byte[] buffer = new byte[32 * 1024];

                for (Evidencia evidencia : seleccionadas) {
                    incorporarEvidencia(zip, evidencia, nombresUsados, buffer);
                }
            }
            return temporal;
        } catch (Exception excepcion) {
            eliminarArchivoTemporal(temporal);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible preparar el ZIP completo de la clase", excepcion);
        }
    }

    private void incorporarEvidencia(ZipOutputStream zip, Evidencia evidencia,
            Set<String> nombresUsados, byte[] buffer) throws IOException {
        String nombre = nombreUnico(evidencia, nombresUsados);
        try (InputStream remoto = plataforma.abrirArchivo(
                evidencia.getIdArchivoPlataforma(), null).contenido()) {
            zip.putNextEntry(new ZipEntry(nombre));
            long transferidos = copiarContenido(remoto, zip, evidencia.getTamanioBytes(), buffer);
            zip.closeEntry();
            if (transferidos != evidencia.getTamanioBytes()) {
                throw new IOException("El archivo no coincide con los metadatos");
            }
        }
    }

    private long copiarContenido(InputStream origen, ZipOutputStream zip,
            long tamanioEsperado, byte[] buffer) throws IOException {
        long total = 0;
        int leidos;
        while ((leidos = origen.read(buffer)) != -1) {
            total += leidos;
            if (total > tamanioEsperado || total > maxZipBytes) {
                throw new IOException("El archivo supera el tamaño esperado");
            }
            zip.write(buffer, 0, leidos);
        }
        return total;
    }

    private String nombreUnico(Evidencia evidencia, Set<String> nombresUsados) {
        String original = nombreSeguro(evidencia.getNombreFisico(), evidencia.getIdArchivoPlataforma());
        String nombre = original;
        int sufijo = 1;
        while (!nombresUsados.add(nombre.toLowerCase(Locale.ROOT))) {
            nombre = evidencia.getIdArchivoPlataforma() + "_" + sufijo + "_" + original;
            sufijo++;
        }
        return nombre;
    }

    private String nombreSeguro(String original, Long idExterno) {
        String limpio = original.replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (limpio.isBlank() || limpio.equals(".") || limpio.equals("..")) {
            return "evidencia_" + idExterno;
        }
        return limpio;
    }

    private void eliminarArchivoTemporal(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignorada) {
            // Si la limpieza falla, el archivo se encuentra en el directorio temporal del servidor.
        }
    }
}
