package org.tfg.backend.seguimiento;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class ConfiguracionSeguimientoService {

    private final ConfiguracionSeguimientoRepository seguimientoRepository;
    private final PlanificacionProyectoRepository planificacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;

    public ConfiguracionSeguimientoService(
            ConfiguracionSeguimientoRepository seguimientoRepository,
            PlanificacionProyectoRepository planificacionRepository,
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository) {

        this.seguimientoRepository = seguimientoRepository;
        this.planificacionRepository = planificacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ResponsableElegibleResponse> listarResponsablesElegibles(Long idProyecto) {
        buscarProyecto(idProyecto);

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<ResponsableElegibleResponse> respuesta = new ArrayList<>();

        for (Usuario usuario : usuarios) {
            if (esResponsableElegible(usuario, idProyecto)) {
                respuesta.add(new ResponsableElegibleResponse(
                        usuario.getIdUsuario(), usuario.getNombres(),
                        usuario.getApellidos(), usuario.getEmail(), usuario.getRol()));
            }
        }

        respuesta.sort(new ComparadorResponsables());
        return respuesta;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionSeguimientoResponse obtener(Long idProyecto) {
        buscarProyecto(idProyecto);

        Optional<ConfiguracionSeguimiento> encontrada = seguimientoRepository.findByProyecto_IdProyecto(idProyecto);
        if (encontrada.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuración de seguimiento no encontrada");
        }

        return convertirRespuesta(encontrada.get());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ConfiguracionSeguimientoResponse guardar(Long idProyecto, ConfiguracionSeguimientoRequest datos) {
        validarDatos(datos);

        Proyecto proyecto = buscarProyecto(idProyecto);
        if (planificacionRepository.findByProyecto_IdProyecto(idProyecto).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Primero debés guardar la planificación del proyecto");
        }

        Usuario responsableSemanal = buscarResponsableElegible(datos.idResponsableSeguimiento(), idProyecto);
        Usuario responsableMensual = buscarResponsableElegible(datos.idResponsableProyecto(), idProyecto);

        Optional<ConfiguracionSeguimiento> encontrada = seguimientoRepository.findByProyecto_IdProyecto(idProyecto);
        ConfiguracionSeguimiento configuracion;

        if (encontrada.isPresent()) {
            configuracion = encontrada.get();
        } else {
            configuracion = new ConfiguracionSeguimiento(proyecto);
        }

        configuracion.actualizarDatos(datos, responsableSemanal, responsableMensual);
        configuracion = seguimientoRepository.save(configuracion);

        // Este cambio y el guardado anterior pertenecen a la misma transacción.
        proyecto.marcarPlanificado();

        return convertirRespuesta(configuracion);
    }

    private Proyecto buscarProyecto(Long idProyecto) {
        if (idProyecto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del proyecto es obligatorio");
        }

        Optional<Proyecto> encontrado = proyectoRepository.findById(idProyecto);
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return encontrado.get();
    }

    private Usuario buscarResponsableElegible(Long idUsuario, Long idProyecto) {
        Optional<Usuario> encontrado = usuarioRepository.findById(idUsuario);
        if (encontrado.isEmpty() || !esResponsableElegible(encontrado.get(), idProyecto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El responsable seleccionado debe ser un usuario interno habilitado y asignado al proyecto si es operativo");
        }
        return encontrado.get();
    }

    private boolean esResponsableElegible(Usuario usuario, Long idProyecto) {
        if (!usuario.isHabilitado()) {
            return false;
        }

        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            return true;
        }

        if (usuario.getRol() != RolUsuario.OPERATIVO) {
            return false;
        }

        for (Proyecto asignado : usuario.getProyectosAsignados()) {
            if (idProyecto.equals(asignado.getIdProyecto())) {
                return true;
            }
        }
        return false;
    }

    private void validarDatos(ConfiguracionSeguimientoRequest datos) {
        if (datos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de seguimiento son obligatorios");
        }

        if (datos.umbralProximoAtrasoSemanas() == null || datos.umbralAtrasoSemanas() == null
                || datos.umbralProximoAtrasoSemanas() <= 0
                || datos.umbralAtrasoSemanas() <= datos.umbralProximoAtrasoSemanas()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El umbral de próximo atraso debe ser positivo y menor que el de atraso");
        }

        if (datos.idResponsableSeguimiento() == null || datos.idResponsableProyecto() == null
                || datos.diaInformeSemanal() == null || datos.tipoDiaInformeMensual() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completá todos los campos obligatorios");
        }

        if (datos.tipoDiaInformeMensual() == TipoDiaInformeMensual.DIA_DEL_MES) {
            if (datos.diaInformeMensual() == null
                    || datos.diaInformeMensual() < 1 || datos.diaInformeMensual() > 31) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El día del informe mensual debe estar entre 1 y 31");
            }
        } else if (datos.diaInformeMensual() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se debe indicar un día numérico cuando se elige el último día hábil");
        }
    }

    private ConfiguracionSeguimientoResponse convertirRespuesta(ConfiguracionSeguimiento configuracion) {
        return new ConfiguracionSeguimientoResponse(
                configuracion.getIdConfiguracionSeguimiento(),
                configuracion.getProyecto().getIdProyecto(),
                configuracion.getUmbralProximoAtrasoSemanas(),
                configuracion.getUmbralAtrasoSemanas(),
                configuracion.getResponsableSeguimiento().getIdUsuario(),
                configuracion.getDiaInformeSemanal(),
                configuracion.getResponsableProyecto().getIdUsuario(),
                configuracion.getTipoDiaInformeMensual(),
                configuracion.getDiaInformeMensual(),
                configuracion.getProyecto().getEstado());
    }
}
