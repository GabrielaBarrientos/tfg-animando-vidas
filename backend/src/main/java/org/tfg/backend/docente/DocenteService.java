package org.tfg.backend.docente;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class DocenteService {
    private static final Pattern FORMATO_EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final DocenteRepository docenteRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public DocenteService(DocenteRepository docenteRepository,
            ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository) {
        this.docenteRepository = docenteRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public List<DocenteResponse> listar(Long idProyecto, OidcUser usuarioGoogle) {
        buscarProyecto(idProyecto);
        verificarAccesoConsulta(idProyecto, usuarioGoogle);

        List<Docente> docentes =
                docenteRepository.findByProyecto_IdProyectoOrderByApellidosAscNombresAsc(idProyecto);
        List<DocenteResponse> resultado = new ArrayList<>();
        for (Docente docente : docentes) {
            resultado.add(convertirRespuesta(docente));
        }
        return resultado;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DocenteResponse crear(Long idProyecto, DocenteRequest datos) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        validarDatos(datos);
        String email = normalizarEmail(datos.email());
        if (docenteRepository.existsByProyecto_IdProyectoAndEmailIgnoreCase(idProyecto, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un docente con este correo en el proyecto");
        }

        Docente docente = docenteRepository.saveAndFlush(new Docente(proyecto, datos));
        return convertirRespuesta(docente);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DocenteResponse actualizar(Long idProyecto, Long idDocente, DocenteRequest datos) {
        buscarProyecto(idProyecto);
        validarDatos(datos);
        Docente docente = buscarDocente(idProyecto, idDocente);
        String email = normalizarEmail(datos.email());
        if (docenteRepository.existsByProyecto_IdProyectoAndEmailIgnoreCaseAndIdDocenteNot(
                idProyecto, email, idDocente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un docente con este correo en el proyecto");
        }

        docente.actualizarDatos(datos);
        docenteRepository.flush();
        return convertirRespuesta(docente);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(Long idProyecto, Long idDocente) {
        buscarProyecto(idProyecto);
        Docente docente = buscarDocente(idProyecto, idDocente);
        if (docenteRepository.tieneTalleresAsociados(idDocente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el docente porque tiene talleres asociados");
        }
        docenteRepository.delete(docente);
        docenteRepository.flush();
    }

    private Proyecto buscarProyecto(Long idProyecto) {
        if (idProyecto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proyecto es obligatorio");
        }
        Optional<Proyecto> encontrado = proyectoRepository.findById(idProyecto);
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return encontrado.get();
    }

    private Docente buscarDocente(Long idProyecto, Long idDocente) {
        if (idDocente == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El docente es obligatorio");
        }
        Optional<Docente> encontrado =
                docenteRepository.findByIdDocenteAndProyecto_IdProyecto(idDocente, idProyecto);
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado en el proyecto");
        }
        return encontrado.get();
    }

    private void verificarAccesoConsulta(Long idProyecto, OidcUser usuarioGoogle) {
        if (usuarioGoogle == null) {
            throw new AccessDeniedException("No se pudo identificar al usuario autenticado");
        }
        Optional<Usuario> encontrado = usuarioRepository.findByGoogleSub(usuarioGoogle.getSubject());
        if (encontrado.isEmpty() || !encontrado.get().isHabilitado()) {
            throw new AccessDeniedException("Usuario no autorizado");
        }

        Usuario usuario = encontrado.get();
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (usuario.getRol() != RolUsuario.OPERATIVO) {
            throw new AccessDeniedException("No tenés permiso para consultar docentes");
        }
        for (Proyecto asignado : usuario.getProyectosAsignados()) {
            if (idProyecto.equals(asignado.getIdProyecto())) {
                return;
            }
        }
        throw new AccessDeniedException("El proyecto no está asignado a tu usuario");
    }

    private void validarDatos(DocenteRequest datos) {
        if (datos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los datos del docente son obligatorios");
        }
        validarCampo(datos.nombres(), "Nombres", 200);
        validarCampo(datos.apellidos(), "Apellidos", 200);
        validarCampo(datos.email(), "Correo electrónico", 320);
        validarCampo(datos.pais(), "País", 100);
        validarCampo(datos.provincia(), "Provincia / región", 150);
        validarCampo(datos.centroEducativo(), "Centro educativo", 200);

        if (!FORMATO_EMAIL.matcher(datos.email().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo electrónico no tiene un formato válido");
        }
    }

    private void validarCampo(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank() || valor.trim().length() > longitudMaxima) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " es obligatorio y admite hasta "
                            + longitudMaxima + " caracteres");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private DocenteResponse convertirRespuesta(Docente docente) {
        return new DocenteResponse(docente.getIdDocente(), docente.getProyecto().getIdProyecto(),
                docente.getNombres(), docente.getApellidos(), docente.getEmail(),
                docente.getPais(), docente.getProvincia(), docente.getCentroEducativo(),
                docenteRepository.tieneTalleresAsociados(docente.getIdDocente()));
    }
}
