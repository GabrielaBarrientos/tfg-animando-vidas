
package org.tfg.backend.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class ProyectoConsultaService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProyectoConsultaService(ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository) {

        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public List<ProyectoResponse> listarProyectos(OidcUser usuarioGoogle) {

        // 1. Verificar que se recibió un usuario autenticado.
        if (usuarioGoogle == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        // 2. Buscar el usuario registrado en la aplicación.
        String googleSub = usuarioGoogle.getSubject();

        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByGoogleSub(googleSub);

        if (usuarioEncontrado.isEmpty()) {
            throw new AccessDeniedException("Usuario no autorizado");
        }

        Usuario usuario = usuarioEncontrado.get();

        if (!usuario.isHabilitado()) {
            throw new AccessDeniedException("Usuario deshabilitado");
        }

        // 3. Consultar los proyectos según el rol.
        List<Proyecto> proyectos;

        switch (usuario.getRol()) {

            case ADMINISTRADOR:
                proyectos = proyectoRepository.findAll();
                break;

            case OPERATIVO:
                proyectos = proyectoRepository.buscarProyectosAsignados(usuario.getIdUsuario());
                break;

            case EXTERNO:

                if (usuario.getIdEmpresa() == null) {
                    throw new AccessDeniedException("El usuario externo no tiene una empresa asociada");
                }

                proyectos = proyectoRepository.findByEmpresa_IdEmpresa(usuario.getIdEmpresa());
                break;

            default:
                throw new AccessDeniedException("Rol de usuario no autorizado");
        }

        // 4. Preparar la respuesta para el frontend.
        List<ProyectoResponse> respuesta = new ArrayList<>();

        for (Proyecto proyecto : proyectos) {

            ProyectoResponse proyectoResponse = new ProyectoResponse(proyecto);

            respuesta.add(proyectoResponse);
        }

        return respuesta;
    }
}
