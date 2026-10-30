
package org.tfg.backend.security;

import java.util.HashSet;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import org.tfg.backend.usuario.UsuarioAutorizacionService;
import org.springframework.security.core.GrantedAuthority;

@Component
public class UsuarioAutorizadoOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService oidcUserService = new OidcUserService();

    private final UsuarioAutorizacionService autorizacionService;

    public UsuarioAutorizadoOidcUserService(UsuarioAutorizacionService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = oidcUserService.loadUser(userRequest);

        return autorizarYAsignarRol(oidcUser);
    }

    OidcUser autorizarYAsignarRol(OidcUser oidcUser) {

    var usuario = autorizacionService
            .buscarUsuarioAutorizado(oidcUser.getSubject())
            .orElseThrow(() -> new OAuth2AuthenticationException(
                    new OAuth2Error("user_not_authorized"),
                    "El usuario no está autorizado para acceder"
            ));

    var autoridades = new HashSet<GrantedAuthority>(
            oidcUser.getAuthorities()
    );

    autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

    return new DefaultOidcUser(
            autoridades,
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            "sub"
    );
}