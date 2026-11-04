package org.tfg.backend.proyecto;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.seguimiento.ConfiguracionSeguimiento;
import org.tfg.backend.seguimiento.ConfiguracionSeguimientoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class DetalleProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanificacionProyectoRepository planificacionRepository;
    private final ConfiguracionSeguimientoRepository seguimientoRepository;
    private final DocenteProyectoRepository docenteRepository;

    public DetalleProyectoService(
            ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository,
            PlanificacionProyectoRepository planificacionRepository,
            ConfiguracionSeguimientoRepository seguimientoRepository,
            DocenteProyectoRepository docenteRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.planificacionRepository = planificacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.docenteRepository = docenteRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public DetalleProyectoResponse obtener(Long idProyecto, OidcUser usuarioGoogle) {
        if (idProyecto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proyecto es obligatorio");
        }

        Optional<Proyecto> proyectoEncontrado = proyectoRepository.findById(idProyecto);
        if (proyectoEncontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        Proyecto proyecto = proyectoEncontrado.get();

        verificarAcceso(proyecto, usuarioGoogle);

        DetalleProyectoResponse.PlanificacionResumen planificacion = null;
        Optional<PlanificacionProyecto> planificacionEncontrada =
                planificacionRepository.findByProyecto_IdProyecto(idProyecto);
        if (planificacionEncontrada.isPresent()) {
            PlanificacionProyecto datos = planificacionEncontrada.get();
            long meses = ChronoUnit.MONTHS.between(
                    YearMonth.from(datos.getFechaInicio()),
                    YearMonth.from(datos.getFechaFinalizacion())) + 1;
            planificacion = new DetalleProyectoResponse.PlanificacionResumen(
                    datos.getFechaInicio(), datos.getFechaFinalizacion(),
                    datos.getCantidadClases(), datos.getFrecuenciaClases().name(), meses);
        }

        DetalleProyectoResponse.SeguimientoResumen seguimiento = null;
        Optional<ConfiguracionSeguimiento> seguimientoEncontrado =
                seguimientoRepository.findByProyecto_IdProyecto(idProyecto);
        if (seguimientoEncontrado.isPresent()) {
            ConfiguracionSeguimiento datos = seguimientoEncontrado.get();
            seguimiento = new DetalleProyectoResponse.SeguimientoResumen(
                    datos.getUmbralProximoAtrasoSemanas(),
                    datos.getUmbralAtrasoSemanas(),
                    nombreCompleto(datos.getResponsableSeguimiento()),
                    datos.getDiaInformeSemanal().name(),
                    nombreCompleto(datos.getResponsableProyecto()),
                    datos.getTipoDiaInformeMensual().name(),
                    datos.getDiaInformeMensual());
        }

        long cantidadDocentes = docenteRepository.countByProyecto_IdProyecto(idProyecto);
        return new DetalleProyectoResponse(
                new ProyectoResponse(proyecto), proyecto.getFechaActualizacion(),
                planificacion, seguimiento, cantidadDocentes);
    }

    private void verificarAcceso(Proyecto proyecto, OidcUser usuarioGoogle) {
        if (usuarioGoogle == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.findByGoogleSub(usuarioGoogle.getSubject());
        if (usuarioEncontrado.isEmpty() || !usuarioEncontrado.get().isHabilitado()) {
            throw new AccessDeniedException("Usuario no autorizado");
        }

        Usuario usuario = usuarioEncontrado.get();
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (usuario.getRol() == RolUsuario.EXTERNO) {
            if (usuario.getIdEmpresa() != null &&
                    usuario.getIdEmpresa().equals(proyecto.getEmpresa().getIdEmpresa())) {
                return;
            }
            throw new AccessDeniedException("El proyecto no pertenece a la empresa del usuario");
        }
        if (usuario.getRol() == RolUsuario.OPERATIVO) {
            for (Proyecto proyectoAsignado : usuario.getProyectosAsignados()) {
                if (proyecto.getIdProyecto().equals(proyectoAsignado.getIdProyecto())) {
                    return;
                }
            }
        }
        throw new AccessDeniedException("No tenés acceso al proyecto");
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario.getNombres() + " " + usuario.getApellidos();
    }
}
