package org.tfg.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuth2LoginFailureHandlerTests {

    private final OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler();

    @Test
    void usuarioSinPermisoDebeVolverAlPrototipo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException error = new OAuth2AuthenticationException(
                new OAuth2Error("user_not_authorized"), "Usuario no habilitado");

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, error);

        assertEquals(302, response.getStatus());
        assertEquals("http://localhost:5173/?acceso=no-autorizado", response.getRedirectedUrl());
    }

    @Test
    void errorDeGoogleDebeMostrarMensajeGeneral() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException error = new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token"), "Fallo de Google");

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, error);

        assertEquals(302, response.getStatus());
        assertEquals("http://localhost:5173/?acceso=error-google", response.getRedirectedUrl());
    }

    @Test
    void errorDeCredencialesNoDebeExponerPantallaDeSpring() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException error = new BadCredentialsException("Credenciales inválidas");

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, error);

        assertEquals(302, response.getStatus());
        assertEquals("http://localhost:5173/?acceso=error-google", response.getRedirectedUrl());
    }
}
