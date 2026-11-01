
package org.tfg.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

public class ApiExceptionHandlingCustomizer
        implements Customizer<ExceptionHandlingConfigurer<HttpSecurity>> {

    @Override
    public void customize(ExceptionHandlingConfigurer<HttpSecurity> configuracion) {

        HttpStatusEntryPoint entradaNoAutenticada = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        PathPatternRequestMatcher rutasApi = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

        configuracion.defaultAuthenticationEntryPointFor(entradaNoAutenticada, rutasApi);
    }
}
