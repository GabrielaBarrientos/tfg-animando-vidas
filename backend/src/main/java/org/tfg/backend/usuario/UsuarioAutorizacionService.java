
package org.tfg.backend.usuario;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class UsuarioAutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> buscarUsuarioAutorizado(String googleSub) {

        if (googleSub == null || googleSub.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository
                .findByGoogleSub(googleSub)
                .filter(Usuario::isHabilitado);
    }
}
