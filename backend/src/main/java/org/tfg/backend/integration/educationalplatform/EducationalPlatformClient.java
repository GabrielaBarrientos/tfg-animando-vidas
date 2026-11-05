
package org.tfg.backend.integration.educationalplatform;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EducationalPlatformClient {

    private final RestClient restClient;

    public EducationalPlatformClient(
            RestClient.Builder restClientBuilder,
            @Value("${educational-platform.base-url}") String baseUrl) {

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
                .uri("/proyecto?nombreProyecto={nombreProyecto}&anioProyecto={anioProyecto}&nombreEmpresa={nombreEmpresa}",
                        nombreProyecto, anioProyecto, nombreEmpresa)
                .retrieve()
                .body(PlatformTallerResponse[].class);
        if (datos == null) {
            throw new IllegalStateException("La plataforma devolvió una respuesta vacía");
        }
        return Arrays.asList(datos);
    }

    public PlatformDetalleTallerResponse obtenerDetalleTaller(int idTallerPlataforma) {
        PlatformDetalleTallerResponse detalle = restClient.get()
                .uri("/taller?id_taller_plataforma={idTallerPlataforma}", idTallerPlataforma)
                .retrieve()
                .body(PlatformDetalleTallerResponse.class);
        if (detalle == null) {
            throw new IllegalStateException("La plataforma no devolvió el detalle del taller");
        }
        return detalle;
    }

    public record PlatformHealthResponse(
            String status,
            String service
    ) {}
}
