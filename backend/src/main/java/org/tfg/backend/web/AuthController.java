
package org.tfg.backend.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(@AuthenticationPrincipal OidcUser user) {

        String rol = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.equals("ROLE_ADMINISTRADOR")
                        || authority.equals("ROLE_OPERATIVO")
                        || authority.equals("ROLE_EXTERNO"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuario sin rol institucional"
                ));

        return new AuthenticatedUserResponse(
                user.getSubject(),
                user.getFullName(),
                user.getEmail(),
                rol
        );
    }

    public record AuthenticatedUserResponse(
            String subject,
            String name,
            String email,
            String rol
    ) {}
}
