
package org.tfg.backend.integration.educationalplatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "RUN_PLATFORM_LIVE_TEST",
        matches = "true"
)
class EducationalPlatformLiveIntegrationTests {

    @Autowired
    private EducationalPlatformClient client;

    @Test
    void shouldConnectToEducationalPlatform() {

        var response = client.checkHealth();

        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("api-simulada", response.service());
    }
}
