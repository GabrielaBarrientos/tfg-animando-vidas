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
import org.tfg.backend.taller.TallerRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class DocenteService {
    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final DocenteRepository docenteRepository;
    private final DocenteProyectoRepository asociacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TallerRepository tallerRepository;
    private final GeneradorUsuarioPlataforma generador;

    public DocenteService(DocenteRepository docenteRepository, DocenteProyectoRepository asociacionRepository,
            ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository,
            TallerRepository tallerRepository, GeneradorUsuarioPlataforma generador) {
        this.docenteRepository = docenteRepository;
        this.asociacionRepository = asociacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tallerRepository = tallerRepository;
        this.generador = generador;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public List<DocenteResponse> listar(Long idProyecto, OidcUser usuarioGoogle) {
        buscarProyecto(idProyecto);
        verificarAccesoConsulta(idProyecto, usuarioGoogle);
        List<DocenteProyecto> asociaciones = asociacionRepository
                .findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(idProyecto);
        List<DocenteResponse> resultado = new ArrayList<>();
        for (DocenteProyecto asociacion : asociaciones) resultado.add(convertirRespuesta(asociacion));
        return resultado;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DocenteResponse crear(Long idProyecto, DocenteRequest datos) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        validarDatos(datos);
        String email = normalizarEmail(datos.email());
        if (asociacionRepository.existsByProyecto_IdProyectoAndDocente_EmailIgnoreCase(idProyecto, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un docente con este correo en el proyecto");
        }
        String usuario = obtenerUsuario(datos);
        verificarUnicidadUsuario(idProyecto, usuario, null);

        Optional<Docente> existente = docenteRepository.findByEmailIgnoreCase(email);
        Docente docente;
        if (existente.isPresent()) {
            docente = existente.get();
            // No sobreescribir silenciosamente los datos personales de otro proyecto.
            if (!docente.getNombres().equalsIgnoreCase(datos.nombres().trim())
                    || !docente.getApellidos().equalsIgnoreCase(datos.apellidos().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El correo ya pertenece a otro docente. Verificá sus datos personales");
            }
        } else {
            docente = docenteRepository.saveAndFlush(new Docente(datos));
        }
        DocenteProyecto asociacion = asociacionRepository.saveAndFlush(
                new DocenteProyecto(docente, proyecto, datos, usuario));
        return convertirRespuesta(asociacion);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DocenteResponse actualizar(Long idProyecto, Long idDocente, DocenteRequest datos) {
        buscarProyecto(idProyecto);
        validarDatos(datos);
        DocenteProyecto asociacion = buscarAsociacion(idProyecto, idDocente);
        Docente docente = asociacion.getDocente();
        String email = normalizarEmail(datos.email());
        Optional<Docente> mismoCorreo = docenteRepository.findByEmailIgnoreCase(email);
        if (mismoCorreo.isPresent() && !mismoCorreo.get().getIdDocente().equals(idDocente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo pertenece a otro docente");
        }
        String usuario = obtenerUsuario(datos);
        verificarUnicidadUsuario(idProyecto, usuario, idDocente);
        docente.actualizarDatos(datos);
        asociacion.actualizarDatos(datos, usuario);
        docenteRepository.flush();
        asociacionRepository.flush();
        return convertirRespuesta(asociacion);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(Long idProyecto, Long idDocente) {
        buscarProyecto(idProyecto);
        DocenteProyecto asociacion = buscarAsociacion(idProyecto, idDocente);
        if (tallerRepository.existsByAsignacion_Docente_IdDocenteAndAsignacion_Proyecto_IdProyecto(idDocente, idProyecto)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desvincular el docente porque tiene un taller en este proyecto");
        }
        asociacionRepository.delete(asociacion);
        asociacionRepository.flush();
        if (!asociacionRepository.existsByDocente_IdDocente(idDocente)) {
            docenteRepository.delete(asociacion.getDocente());
            docenteRepository.flush();
        }
    }

    private String obtenerUsuario(DocenteRequest datos) {
        String usuario = datos.usuarioPlataforma();
        if (usuario == null || usuario.isBlank()) {
            usuario = generador.generar(datos.nombres(), datos.apellidos());
        } else {
            usuario = usuario.trim().toLowerCase(Locale.ROOT);
        }
        if (!generador.formatoValido(usuario)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario debe tener al menos 8 caracteres, letras minúsculas y un punto; sin números");
        }
        return usuario;
    }

    private void verificarUnicidadUsuario(Long idProyecto, String usuario, Long idDocente) {
        boolean existe;
        if (idDocente == null) {
            existe = asociacionRepository.existsByProyecto_IdProyectoAndUsuarioPlataformaIgnoreCase(idProyecto, usuario);
        } else {
            existe = asociacionRepository
                    .existsByProyecto_IdProyectoAndUsuarioPlataformaIgnoreCaseAndDocente_IdDocenteNot(
                            idProyecto, usuario, idDocente);
        }
        if (existe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario de plataforma ya existe en este proyecto. Modificalo manualmente");
        }
    }

    private Proyecto buscarProyecto(Long idProyecto) {
        if (idProyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proyecto es obligatorio");
        Optional<Proyecto> encontrado = proyectoRepository.findById(idProyecto);
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return encontrado.get();
    }

    private DocenteProyecto buscarAsociacion(Long idProyecto, Long idDocente) {
        if (idDocente == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El docente es obligatorio");
        Optional<DocenteProyecto> asociado = asociacionRepository
                .findByDocente_IdDocenteAndProyecto_IdProyecto(idDocente, idProyecto);
        if (asociado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado en el proyecto");
        }
        return asociado.get();
    }

    private void verificarAccesoConsulta(Long idProyecto, OidcUser usuarioGoogle) {
        if (usuarioGoogle == null) throw new AccessDeniedException("Usuario no autenticado");
        Optional<Usuario> encontrado = usuarioRepository.findByGoogleSub(usuarioGoogle.getSubject());
        if (encontrado.isEmpty() || !encontrado.get().isHabilitado()) {
            throw new AccessDeniedException("Usuario no autorizado");
        }
        Usuario usuario = encontrado.get();
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) return;
        if (usuario.getRol() != RolUsuario.OPERATIVO) {
            throw new AccessDeniedException("No tenés permiso para consultar docentes");
        }
        for (Proyecto asignado : usuario.getProyectosAsignados()) {
            if (idProyecto.equals(asignado.getIdProyecto())) return;
        }
        throw new AccessDeniedException("El proyecto no está asignado a tu usuario");
    }

    private void validarDatos(DocenteRequest datos) {
        if (datos == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos del docente obligatorios");
        validarCampo(datos.nombres(), "Nombres", 200);
        validarCampo(datos.apellidos(), "Apellidos", 200);
        validarCampo(datos.email(), "Correo", 320);
        validarCampo(datos.telefono(), "Teléfono", 30);
        validarCampo(datos.pais(), "País", 100);
        validarCampo(datos.provincia(), "Provincia", 150);
        validarCampo(datos.centroEducativo(), "Centro educativo", 200);
        validarCampo(datos.cargo(), "Cargo", 150);
        validarCampo(datos.grado(), "Grado", 100);
        validarCampo(datos.materia(), "Materia", 150);
        if (!FORMATO_EMAIL.matcher(datos.email().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico no tiene formato válido");
        }
        if (datos.cargaHoraria() == null || datos.cargaHoraria() <= 0
                || datos.cantidadAlumnos() == null || datos.cantidadAlumnos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La carga horaria y cantidad de alumnos deben ser enteros mayores que cero");
        }
    }

    private void validarCampo(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " es obligatorio y admite hasta " + maximo + " caracteres");
        }
    }

    private String normalizarEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private DocenteResponse convertirRespuesta(DocenteProyecto asociacion) {
        Docente docente = asociacion.getDocente();
        Long id = docente.getIdDocente();
        boolean conTaller = tallerRepository.existsByAsignacion_Docente_IdDocenteAndAsignacion_Proyecto_IdProyecto(
                id, asociacion.getProyecto().getIdProyecto());
        return new DocenteResponse(id, asociacion.getProyecto().getIdProyecto(),
                docente.getNombres(), docente.getApellidos(), docente.getEmail(), docente.getTelefono(),
                asociacion.getPais(), asociacion.getProvincia(), asociacion.getCentroEducativo(),
                asociacion.getCargo(), asociacion.getGrado(), asociacion.getMateria(),
                asociacion.getCargaHoraria(), asociacion.getCantidadAlumnos(),
                asociacion.getUsuarioPlataforma(), conTaller);
    }
}
