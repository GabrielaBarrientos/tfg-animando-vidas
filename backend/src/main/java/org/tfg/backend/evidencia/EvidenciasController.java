package org.tfg.backend.evidencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/talleres/{idTaller}")
public class EvidenciasController {
    private final EvidenciasService servicio;
    private final EvidenciasZipService zipService;
    private final EducationalPlatformClient plataforma;

    public EvidenciasController(EvidenciasService servicio, EvidenciasZipService zipService,
            EducationalPlatformClient plataforma) {
        this.servicio = servicio;
        this.zipService = zipService;
        this.plataforma = plataforma;
    }

    @GetMapping("/evidencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public EvidenciasResponse listar(@PathVariable Long idProyecto, @PathVariable String idTaller,
            @AuthenticationPrincipal OidcUser google) {
        return servicio.consultar(idProyecto, idTaller, google);
    }

    @PostMapping("/evidencias/sincronizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public SincronizacionResponse sincronizar(@PathVariable Long idProyecto, @PathVariable String idTaller,
            @AuthenticationPrincipal OidcUser google) {
        return new SincronizacionResponse(servicio.sincronizar(idProyecto, idTaller, google));
    }

    @PatchMapping("/evidencias/{idEvidencia}/marcadores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public EvidenciasResponse.Archivo marcar(@PathVariable Long idProyecto, @PathVariable String idTaller,
            @PathVariable Long idEvidencia, @RequestBody MarcadoresRequest solicitud,
            @AuthenticationPrincipal OidcUser google) {
        return servicio.actualizarMarcadores(idProyecto, idTaller, idEvidencia, solicitud, google);
    }

    @GetMapping("/evidencias/{idEvidencia}/visualizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public ResponseEntity<StreamingResponseBody> visualizar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @PathVariable Long idEvidencia,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rango,
            @AuthenticationPrincipal OidcUser google) {
        Evidencia evidencia = servicio.obtenerArchivoAutorizado(idProyecto, idTaller, idEvidencia, google);
        return prepararRespuesta(evidencia, rango, false);
    }

    @GetMapping("/evidencias/{idEvidencia}/descargar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public ResponseEntity<StreamingResponseBody> descargar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @PathVariable Long idEvidencia,
            @AuthenticationPrincipal OidcUser google) {
        Evidencia evidencia = servicio.obtenerArchivoParaDescargar(idProyecto, idTaller, idEvidencia, google);
        return prepararRespuesta(evidencia, null, true);
    }

    @GetMapping("/clases/{idClaseTaller}/evidencias/zip")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public ResponseEntity<StreamingResponseBody> descargarZip(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @PathVariable Long idClaseTaller,
            @AuthenticationPrincipal OidcUser google) {
        Path archivo = zipService.generarZip(idProyecto, idTaller, idClaseTaller, google);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("taller_" + idTaller + "_clase_" + idClaseTaller + "_evidencias.zip",
                            StandardCharsets.UTF_8).build());
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentLength(Files.size(archivo));
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(new TransferenciaBinaria(archivo), headers, HttpStatus.OK);
        } catch (IOException excepcion) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignorada) {
                // El archivo temporal será depurado por el sistema operativo.
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo entregar el ZIP", excepcion);
        }
    }

    private ResponseEntity<StreamingResponseBody> prepararRespuesta(Evidencia evidencia,
            String rango, boolean descargar) {
        if (!EvidenciasService.tipoPermitido(evidencia.getMimeType())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tipo de archivo no permitido");
        }
        EducationalPlatformClient.ArchivoRemoto remoto = plataforma.abrirArchivo(
                evidencia.getIdArchivoPlataforma(), rango);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(evidencia.getMimeType()));
        if (remoto.longitud() >= 0) {
            headers.setContentLength(remoto.longitud());
        }
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (remoto.rangoContenido() != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, remoto.rangoContenido());
        }
        if (descargar) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(evidencia.getNombreFisico(), StandardCharsets.UTF_8).build());
        } else {
            headers.setContentDisposition(ContentDisposition.inline().build());
        }
        headers.setCacheControl("no-store");
        HttpStatus estado = remoto.estado() == 206 ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(new TransferenciaBinaria(remoto.contenido()), headers, estado);
    }

    public record SincronizacionResponse(int cantidadEvidencias) { }
}
