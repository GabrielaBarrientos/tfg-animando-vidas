
package org.tfg.backend.integration.educationalplatform;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EducationalPlatformClientTests {

    @Test
    void shouldRetrievePlatformHealth() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(builder).build();

        EducationalPlatformClient client =
                new EducationalPlatformClient(
                        builder,
                        "http://127.0.0.1:8000"
                );

        mockServer.expect(
                requestTo("http://127.0.0.1:8000/health")
        )
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
                """
                {
                    "status": "ok",
                    "service": "api-simulada"
                }
                """,
                MediaType.APPLICATION_JSON
        ));

        var response = client.checkHealth();

        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("api-simulada", response.service());

        mockServer.verify();
    }
}
