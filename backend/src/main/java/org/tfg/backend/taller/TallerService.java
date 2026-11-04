package org.tfg.backend.taller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.docente.DocenteProyecto;
import org.tfg.backend.docente.DocenteProyectoRepository;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformTallerResponse;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.seguimiento.ConfiguracionSeguimiento;
import org.tfg.backend.seguimiento.ConfiguracionSeguimientoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class TallerService {
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteProyectoRepository asociacionRepository;
    private final TallerRepository tallerRepository;
    private final ClaseTallerRepository claseRepository;
    private final PlanificacionProyectoRepository planificacionRepository;
    private final ConfiguracionSeguimientoRepository seguimientoRepository;
    private final EducationalPlatformClient platformClient;
    private final TallerSeguimientoCalculador calculador;

    public TallerService(ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository,
            DocenteProyectoRepository asociacionRepository, TallerRepository tallerRepository,
            ClaseTallerRepository claseRepository, PlanificacionProyectoRepository planificacionRepository,
            ConfiguracionSeguimientoRepository seguimientoRepository,
            EducationalPlatformClient platformClient, TallerSeguimientoCalculador calculador) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asociacionRepository = asociacionRepository;
        this.tallerRepository = tallerRepository;
        this.claseRepository = claseRepository;
        this.planificacionRepository = planificacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.platformClient = platformClient;
        this.calculador = calculador;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public List<TallerResumenResponse> listar(Long idProyecto, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        Optional<PlanificacionProyecto> plan = planificacionRepository.findByProyecto_IdProyecto(idProyecto);
        Optional<ConfiguracionSeguimiento> seguimiento = seguimientoRepository.findByProyecto_IdProyecto(idProyecto);
        if (plan.isEmpty() || seguimiento.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El proyecto necesita planificación y configuración de seguimiento");
        }
        List<TallerResumenResponse> resultado = new ArrayList<>();
        List<Taller> talleres = tallerRepository.findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(idProyecto);
        for (Taller taller : talleres) {
            List<ClaseTaller> clases = claseRepository.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(
                    taller.getIdTallerPlataforma());
            resultado.add(calculador.calcular(taller, clases, plan.get(), seguimiento.get(), LocalDate.now()));
        }
        return resultado;
    }

    /** Datos de sincronización por proyecto, sin exponer información de otros proyectos. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public TalleresMetadatosResponse obtenerMetadatos(Long idProyecto, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        return new TalleresMetadatosResponse(proyecto.getEstado().name(),
                proyecto.getUltimaSincronizacionTalleres(), proyecto.getCantidadTalleresPlataforma());
    }

    /** Sincronización transaccional: valida el lote completo antes de escribir. */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public int sincronizar(Long idProyecto, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        Optional<PlanificacionProyecto> plan = planificacionRepository.findByProyecto_IdProyecto(idProyecto);
        if (plan.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El proyecto no tiene planificación");

        List<DocenteProyecto> docentes = asociacionRepository
                .findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(idProyecto);
        Map<String, DocenteProyecto> porUsuario = new HashMap<>();
        for (DocenteProyecto docente : docentes) {
            porUsuario.put(docente.getUsuarioPlataforma().toLowerCase(Locale.ROOT), docente);
        }
        List<PlatformTallerResponse> remotos;
        try {
            remotos = platformClient.obtenerTalleres(
                    proyecto.getNombre(), proyecto.getAnio(), proyecto.getEmpresa().getNombre());
        } catch (Exception excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar la plataforma educativa", excepcion);
        }
        if (remotos == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de plataforma");
        Set<String> identificadores = new HashSet<>();
        Set<Long> docentesEnLote = new HashSet<>();
        for (PlatformTallerResponse remoto : remotos) {
            validarRemoto(remoto, proyecto, porUsuario, identificadores, docentesEnLote, plan.get().getCantidadClases());
            Optional<Taller> existente = tallerRepository.findById(remoto.idTaller());
            if (existente.isPresent() && !existente.get().getAsignacion().getProyecto().getIdProyecto().equals(idProyecto)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El identificador del taller ya pertenece a otro proyecto");
            }
        }
        List<Taller> locales = tallerRepository.findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(idProyecto);
        for (Taller local : locales) {
            if (!identificadores.contains(local.getIdTallerPlataforma())) {
                local.marcarPendienteBaja();
            }
        }
        Map<Long, Taller> localPorDocente = new HashMap<>();
        if (!remotos.isEmpty()) {
            for (Taller local : locales) {
                localPorDocente.put(local.getAsignacion().getDocente().getIdDocente(), local);
            }
        }
        for (PlatformTallerResponse remoto : remotos) {
            DocenteProyecto docente = porUsuario.get(remoto.usuarioPlataforma().trim().toLowerCase(Locale.ROOT));
            Taller anteriorDocente = localPorDocente.get(docente.getDocente().getIdDocente());
            if (anteriorDocente != null && !anteriorDocente.getIdTallerPlataforma().equals(remoto.idTaller())) {
                // El docente tiene un nuevo taller remoto; primero se debe confirmar la baja local anterior.
                // La interfaz muestra el anterior marcado pendiente y permite eliminar su copia.
                continue;
            }
            Optional<Taller> encontrado = tallerRepository.findById(remoto.idTaller());
            Taller taller;
            if (encontrado.isPresent()) {
                taller = encontrado.get();
                taller.actualizar(docente);
            } else {
                taller = tallerRepository.save(new Taller(remoto.idTaller(), docente));
            }
            List<ClaseTaller> anteriores = claseRepository.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(remoto.idTaller());
            Map<Integer, ClaseTaller> porNumero = new HashMap<>();
            Set<Integer> numerosRecibidos = new HashSet<>();
            for (ClaseTaller clase : anteriores) porNumero.put(clase.getNumeroClase(), clase);
            for (PlatformTallerResponse.ClasePlataforma dato : remoto.clases()) {
                numerosRecibidos.add(dato.numeroClase());
                ClaseTaller clase = porNumero.get(dato.numeroClase());
                if (clase == null) {
                    clase = new ClaseTaller(taller, dato.numeroClase(), dato.fechaRealizacion(),
                            dato.cantidadParticipantes(), dato.cantidadEvidencias());
                } else {
                    clase.actualizar(dato.fechaRealizacion(), dato.cantidadParticipantes(), dato.cantidadEvidencias());
                }
                claseRepository.save(clase);
            }
            // Clases ausentes del lote: no conservar valores obsoletos.
            for (ClaseTaller anterior : anteriores) {
                if (!numerosRecibidos.contains(anterior.getNumeroClase())) claseRepository.delete(anterior);
            }
        }
        proyecto.registrarSincronizacionTalleres(remotos.size());
        // Las bajas pendientes siguen contando como copias locales hasta ser confirmadas.
        proyecto.actualizarEstadoPorTalleres(!locales.isEmpty() || !remotos.isEmpty());
        return remotos.size();
    }

    private void validarRemoto(PlatformTallerResponse remoto, Proyecto proyecto,
            Map<String, DocenteProyecto> docentes, Set<String> ids,
            Set<Long> docentesEnLote, int cantidadClases) {
        if (remoto == null || remoto.idTaller() == null || remoto.idTaller().isBlank()
                || remoto.idTaller().length() > 150 || !ids.add(remoto.idTaller())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió identificadores de talleres inválidos o repetidos");
        }
        // La plataforma selecciona los talleres mediante nombre, año y empresa.
        // Comprobar el contrato antes de modificar las copias locales.
        if (!proyecto.getNombre().equals(remoto.nombreProyecto())
                || !proyecto.getAnio().equals(remoto.anioProyecto())
                || !proyecto.getEmpresa().getNombre().equals(remoto.nombreEmpresa())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió un taller que no corresponde al proyecto consultado");
        }
        String usuario = remoto.usuarioPlataforma() == null ? "" : remoto.usuarioPlataforma().trim().toLowerCase(Locale.ROOT);
        DocenteProyecto asignacion = docentes.get(usuario);
        if (asignacion == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El taller " + remoto.idTaller() + " no tiene un usuario de plataforma registrado en el proyecto");
        }
        if (!docentesEnLote.add(asignacion.getDocente().getIdDocente())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un docente tiene más de un taller en el mismo proyecto");
        }
        if (remoto.clases() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Clases no informadas para " + remoto.idTaller());
        }
        Set<Integer> numeros = new HashSet<>();
        for (PlatformTallerResponse.ClasePlataforma clase : remoto.clases()) {
            if (clase == null || clase.numeroClase() == null || clase.numeroClase() < 1
                    || clase.numeroClase() > cantidadClases || !numeros.add(clase.numeroClase())
                    || clase.cantidadEvidencias() == null || clase.cantidadEvidencias() < 0
                    || (clase.fechaRealizacion() == null && (clase.cantidadEvidencias() > 0
                    || clase.cantidadParticipantes() != null))
                    || (clase.fechaRealizacion() != null && clase.cantidadParticipantes() == null)
                    || (clase.cantidadParticipantes() != null && (clase.cantidadParticipantes() < 0
                    || clase.cantidadParticipantes() > asignacion.getCantidadAlumnos()))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Clase inválida del taller " + remoto.idTaller());
            }
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public void eliminarCopiaLocal(Long idProyecto, String idTaller, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        Optional<Taller> encontrado = tallerRepository.findById(idTaller);
        if (encontrado.isEmpty() || !encontrado.get().getAsignacion().getProyecto().getIdProyecto().equals(idProyecto)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado en el proyecto");
        }
        Taller taller = encontrado.get();
        if (!taller.isPendienteBaja()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El taller no fue marcado como ausente en la última sincronización");
        }
        claseRepository.deleteByTaller_IdTallerPlataforma(idTaller);
        claseRepository.flush();
        tallerRepository.delete(taller);
        tallerRepository.flush();
        proyecto.actualizarEstadoPorTalleres(tallerRepository.existsByAsignacion_Proyecto_IdProyecto(idProyecto));
    }

    private Proyecto buscarProyecto(Long idProyecto) {
        if (idProyecto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proyecto obligatorio");
        Optional<Proyecto> encontrado = proyectoRepository.findById(idProyecto);
        if (encontrado.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        return encontrado.get();
    }

    private void verificarAcceso(Proyecto proyecto, OidcUser usuarioGoogle) {
        if (usuarioGoogle == null) throw new AccessDeniedException("Usuario no autenticado");
        Optional<Usuario> encontrado = usuarioRepository.findByGoogleSub(usuarioGoogle.getSubject());
        if (encontrado.isEmpty() || !encontrado.get().isHabilitado())
            throw new AccessDeniedException("Usuario no autorizado");
        Usuario usuario = encontrado.get();
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) return;
        if (usuario.getRol() == RolUsuario.EXTERNO && usuario.getIdEmpresa() != null
                && usuario.getIdEmpresa().equals(proyecto.getEmpresa().getIdEmpresa())) return;
        if (usuario.getRol() == RolUsuario.OPERATIVO) {
            for (Proyecto asignado : usuario.getProyectosAsignados()) {
                if (proyecto.getIdProyecto().equals(asignado.getIdProyecto())) return;
            }
        }
        throw new AccessDeniedException("No tenés acceso a este proyecto");
    }
}
