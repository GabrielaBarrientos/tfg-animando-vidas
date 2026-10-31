
package org.tfg.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void administradorPuedeObtenerTokenCsrf() throws Exception {

        SimpleGrantedAuthority autoridad = new SimpleGrantedAuthority("ROLE_ADMINISTRADOR");

        mockMvc.perform(get("/api/csrf")
                        .with(oidcLogin().authorities(autoridad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName")
                        .value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName")
                        .value("_csrf"));
    }

    @Test
    void usuarioSinRolInstitucionalNoPuedeObtenerToken()
            throws Exception {

        mockMvc.perform(get("/api/csrf")
                        .with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postSinTokenCsrfEsRechazado() throws Exception {

        SimpleGrantedAuthority autoridad = new SimpleGrantedAuthority("ROLE_ADMINISTRADOR");

        mockMvc.perform(post("/api/proyectos")
                        .with(oidcLogin().authorities(autoridad))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
