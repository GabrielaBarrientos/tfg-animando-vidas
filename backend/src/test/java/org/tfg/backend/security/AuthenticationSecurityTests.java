
package org.tfg.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import org.tfg.backend.web.AuthController;
import org.tfg.backend.web.HealthController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest({
        AuthController.class,
        HealthController.class
})
@Import({
        SecurityConfig.class,
        AuthenticationSecurityTests.TestOAuthConfig.class
})

class AuthenticationSecurityTests {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UsuarioAutorizadoOidcUserService oidcUserService;

    @Test
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("backend"));
    }

    @Test
    void currentUserShouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserShouldRetrieveIdentity() throws Exception {

        mockMvc.perform(
                get("/api/auth/me")
                    .with(oidcLogin()
                        .authorities(
                            new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")
                        )
                        .idToken(token -> token
                            .claim("sub", "test-user-123")
                            .claim("name", "Usuario de prueba")
                            .claim("email", "test@example.com")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("test-user-123"))
            .andExpect(jsonPath("$.name").value("Usuario de prueba"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestOAuthConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            var google = CommonOAuth2Provider.GOOGLE
                    .getBuilder("google")
                    .clientId("test-client-id")
                    .clientSecret("test-client-secret")
                    .build();

            return new InMemoryClientRegistrationRepository(google);
        }
    }

    @Test
    void csrfEndpointShouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectsEndpointShouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/proyectos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLoginShouldStillRedirect() throws Exception {

        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }

}
