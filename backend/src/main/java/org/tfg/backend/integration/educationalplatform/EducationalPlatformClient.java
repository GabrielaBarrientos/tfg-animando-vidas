
package org.tfg.backend.integration.educationalplatform;

import java.util.Arrays;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EducationalPlatformClient {

    @Value("${educational-platform.file-timeout-seconds:120}")
    private int fileTimeoutSeconds = 120;
    private final RestClient restClient;
    private final HttpClient httpClient;
    private final String baseUrl;

    public EducationalPlatformClient(
            RestClient.Builder restClientBuilder,
            @Value("${educational-platform.base-url}") String baseUrl) {

        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public PlatformHealthResponse checkHealth() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(PlatformHealthResponse.class);
    }

    public List<PlatformTallerResponse> obtenerTalleres(
            String nombreProyecto, Short anioProyecto, String nombreEmpresa) {
        PlatformTallerResponse[] datos = restClient.get()
                .uri("/talleres?nombreProyecto={nombreProyecto}&anioProyecto={anioProyecto}&nombreEmpresa={nombreEmpresa}",
                        nombreProyecto, anioProyecto, nombreEmpresa)
                .retrieve()
                .body(PlatformTallerResponse[].class);
        if (datos == null) {
            throw new IllegalStateException("La plataforma devolvió una respuesta vacía");
        }
        return Arrays.asList(datos);
    }

    public PlatformProgramaResponse obtenerPrograma(String nombreProyecto, String empresa, Short anio) {
        PlatformProgramaResponse respuesta = restClient.get()
                .uri("/programas?nombreProyecto={nombreProyecto}&empresa={empresa}&anio={anio}",
                        nombreProyecto, empresa, anio)
                .retrieve().body(PlatformProgramaResponse.class);
        if (respuesta == null) throw new IllegalStateException("La plataforma no devolvió el programa");
        return respuesta;
    }

    public PlatformEncuestasResponse obtenerEncuestas(int idTallerPlataforma) {
        PlatformEncuestasResponse respuesta = restClient.get()
                .uri("/encuestas?id_taller_plataforma={idTallerPlataforma}", idTallerPlataforma)
                .retrieve()
                .body(PlatformEncuestasResponse.class);
        if (respuesta == null) {
            throw new IllegalStateException("La plataforma no devolvió las encuestas");
        }
        return respuesta;
    }

    public PlatformEvidenciasResponse obtenerEvidencias(int idTallerPlataforma) {
        PlatformEvidenciasResponse respuesta = restClient.get()
                .uri("/evidencias?id_taller_plataforma={idTallerPlataforma}", idTallerPlataforma)
                .retrieve().body(PlatformEvidenciasResponse.class);
        if (respuesta == null) throw new IllegalStateException("La plataforma no devolvió evidencias");
        return respuesta;
    }

    /** Devuelve el InputStream remoto. El llamador debe cerrarlo. */
    public ArchivoRemoto abrirArchivo(long idArchivoPlataforma, String rango) {
        if (idArchivoPlataforma <= 0) throw new IllegalArgumentException("ID de archivo inválido");
        try {
            HttpRequest.Builder solicitud = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/evidencias/" + idArchivoPlataforma + "/archivo"))
                    .timeout(Duration.ofSeconds(fileTimeoutSeconds)).GET();
            if (rango != null && !rango.isBlank()) solicitud.header("Range", rango);
            HttpResponse<InputStream> respuesta = httpClient.send(solicitud.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            int estado = respuesta.statusCode();
            if (estado != 200 && estado != 206) {
                respuesta.body().close();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "No se pudo recuperar el archivo de la plataforma educativa");
            }
            String tipo = respuesta.headers().firstValue("Content-Type").orElse("application/octet-stream");
            String rangoContenido = respuesta.headers().firstValue("Content-Range").orElse(null);
            long longitud = -1;
            try {
                longitud = Long.parseLong(respuesta.headers().firstValue("Content-Length").orElse("-1"));
            } catch (NumberFormatException excepcion) {
                longitud = -1;
            }
            return new ArchivoRemoto(estado, respuesta.body(), tipo, longitud, rangoContenido);
        } catch (ResponseStatusException excepcion) {
            throw excepcion;
        } catch (Exception excepcion) {
            if (excepcion instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo recuperar el archivo de la plataforma educativa", excepcion);
        }
    }

    public record ArchivoRemoto(int estado, InputStream contenido, String tipo,
            long longitud, String rangoContenido) { }

    public record PlatformHealthResponse(
            String status,
            String service
    ) {}
}
