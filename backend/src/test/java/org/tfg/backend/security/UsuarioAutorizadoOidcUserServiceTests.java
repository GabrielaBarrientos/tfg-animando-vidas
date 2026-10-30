
package org.tfg.backend.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioAutorizacionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioAutorizadoOidcUserServiceTests {

    private final UsuarioAutorizacionService autorizacionService =
            mock(UsuarioAutorizacionService.class);

    private final UsuarioAutorizadoOidcUserService service =
            new UsuarioAutorizadoOidcUserService(autorizacionService);

    @ParameterizedTest
    @EnumSource(RolUsuario.class)
    void debeAsignarRolInstitucional(RolUsuario rol) {

        Usuario usuario = new Usuario(
                1L,
                "google-123",
                "test@example.com",
                "Usuario",
                "Prueba",
                rol,
                true,
                null,
                null
        );

        when(autorizacionService
                .buscarUsuarioAutorizado("google-123"))
                .thenReturn(Optional.of(usuario));

        OidcUser resultado = service.autorizarYAsignarRol(crearUsuarioOidc());

        assertTrue(resultado.getAuthorities().stream()
                .anyMatch(autoridad ->
                        autoridad.getAuthority()
                                .equals("ROLE_" + rol.name())
                ));

        assertEquals("google-123", resultado.getSubject());

        assertTrue(resultado.getAuthorities().stream()
                .anyMatch(autoridad ->
                        autoridad.getAuthority().equals("OIDC_USER")
                ));
    }

    @Test
    void debeRechazarUsuarioNoAutorizado() {

        when(autorizacionService
                .buscarUsuarioAutorizado("google-123"))
                .thenReturn(Optional.empty());

        var excepcion = assertThrows(
                OAuth2AuthenticationException.class,
                () -> service.autorizarYAsignarRol(crearUsuarioOidc())
        );

        assertEquals(
                "user_not_authorized",
                excepcion.getError().getErrorCode()
        );
    }

    private OidcUser crearUsuarioOidc() {

        var idToken = new OidcIdToken(
                "token-de-prueba",
                Instant.now(),
                Instant.now().plusSeconds(600),
                Map.of("sub", "google-123")
        );

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("OIDC_USER")),
                idToken
        );
    }
}
