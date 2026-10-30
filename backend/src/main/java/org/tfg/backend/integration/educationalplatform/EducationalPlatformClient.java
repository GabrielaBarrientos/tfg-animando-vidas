
package org.tfg.backend.integration.educationalplatform;

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

    public record PlatformHealthResponse(
            String status,
            String service
    ) {}
}
