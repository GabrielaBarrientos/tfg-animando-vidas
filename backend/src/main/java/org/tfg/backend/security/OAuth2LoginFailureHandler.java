package org.tfg.backend.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Devuelve al usuario a la pantalla del prototipo cuando falla OAuth2.
 * Nunca presenta la pagina de inicio de sesion predeterminada de Spring.
 */
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String URL_ACCESO_NO_AUTORIZADO =
            "http://localhost:5173/?acceso=no-autorizado";

    private static final String URL_ERROR_GOOGLE =
            "http://localhost:5173/?acceso=error-google";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String urlRedireccion = URL_ERROR_GOOGLE;

        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException errorOauth =
                    (OAuth2AuthenticationException) exception;

            if ("user_not_authorized".equals(errorOauth.getError().getErrorCode())) {
                urlRedireccion = URL_ACCESO_NO_AUTORIZADO;
            }
        }

        response.sendRedirect(urlRedireccion);
    }
}
