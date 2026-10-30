
package org.tfg.backend.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(@AuthenticationPrincipal OidcUser user) {

        return new AuthenticatedUserResponse(
                user.getSubject(),
                user.getFullName(),
                user.getEmail()
        );
    }

    public record AuthenticatedUserResponse(
            String subject,
            String name,
            String email
    ) {}
}
