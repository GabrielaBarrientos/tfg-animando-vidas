package org.tfg.backend.taller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.tfg.backend.programa.Programa;
import org.tfg.backend.programa.ProgramaRepository;
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
    private final ProgramaRepository programaRepository;

    public TallerService(ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository,
            DocenteProyectoRepository asociacionRepository, TallerRepository tallerRepository,
            ClaseTallerRepository claseRepository, PlanificacionProyectoRepository planificacionRepository,
            ConfiguracionSeguimientoRepository seguimientoRepository,
            EducationalPlatformClient platformClient, TallerSeguimientoCalculador calculador,
            ProgramaRepository programaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asociacionRepository = asociacionRepository;
        this.tallerRepository = tallerRepository;
        this.claseRepository = claseRepository;
        this.planificacionRepository = planificacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.platformClient = platformClient;
        this.calculador = calculador;
        this.programaRepository = programaRepository;
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

    /** Consulta el taller y todas las clases previstas sin sincronizar con FastAPI. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public DetalleTallerResponse obtenerDetalle(Long idProyecto, String idTaller, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        if (idTaller == null || idTaller.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El taller es obligatorio");
        }
        Optional<Taller> encontrado = tallerRepository.findByIdTallerPlataforma(idTaller);
        if (encontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
        }
        Taller taller = encontrado.get();
        if (!idProyecto.equals(taller.getAsignacion().getProyecto().getIdProyecto())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado en el proyecto");
        }
        Optional<PlanificacionProyecto> plan = planificacionRepository.findByProyecto_IdProyecto(idProyecto);
        Optional<ConfiguracionSeguimiento> seguimiento = seguimientoRepository.findByProyecto_IdProyecto(idProyecto);
        if (plan.isEmpty() || seguimiento.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El proyecto necesita planificación y configuración de seguimiento");
        }

        List<ClaseTaller> clasesGuardadas = claseRepository
                .findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller);
        TallerResumenResponse resumen = calculador.calcular(taller, clasesGuardadas,
                plan.get(), seguimiento.get(), LocalDate.now());
        List<DetalleTallerResponse.ClaseDetalle> clases = new ArrayList<>();
        for (ClaseTaller claseGuardada : clasesGuardadas) {
            int numero = claseGuardada.getNumeroClase();
            LocalDate fechaPrevista = calculador.calcularFechaLimite(
                    plan.get().getFechaInicio(), plan.get().getFrecuenciaClases(), numero);
            DetalleTallerResponse.EstadoClase estado =
                    claseGuardada.getEstadoClase() == EstadoClaseTaller.REALIZADA
                    ? DetalleTallerResponse.EstadoClase.REALIZADA
                    : DetalleTallerResponse.EstadoClase.PENDIENTE;
            clases.add(new DetalleTallerResponse.ClaseDetalle(
                    claseGuardada.getIdClaseTaller(), numero, claseGuardada.getNombreClase(),
                    claseGuardada.getNombreModulo(), fechaPrevista, claseGuardada.getFechaRealizacion(),
                    claseGuardada.getCantidadParticipantes(), claseGuardada.getCantidadEvidencias(), estado));
        }
        DocenteProyecto asociacion = taller.getAsignacion();
        String nombreDocente = asociacion.getDocente().getNombres() + " "
                + asociacion.getDocente().getApellidos();
        return new DetalleTallerResponse(
                taller.getIdTallerPlataforma(), nombreDocente.trim(),
                asociacion.getUsuarioPlataforma(), asociacion.getPais(),
                asociacion.getProvincia(), asociacion.getCentroEducativo(),
                asociacion.getCantidadAlumnos(), plan.get().getCantidadClases(),
                resumen.avanceEsperado(), resumen.avanceReal(), resumen.diferencia(),
                resumen.estado(), resumen.evidencias(), taller.isPendienteBaja(),
                taller.getUltimaSincronizacionClases(), clases);
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

    /** Todos los talleres y sus clases se actualizan desde GET /talleres en una única transacción. */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public int sincronizar(Long idProyecto, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        if (!proyecto.isProgramaSincronizado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Primero sincronizá el programa del proyecto");
        }
        Optional<PlanificacionProyecto> plan = planificacionRepository.findByProyecto_IdProyecto(idProyecto);
        if (plan.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El proyecto no tiene planificación");
        Map<Long, Programa> definiciones = new HashMap<>();
        for (Programa programa : programaRepository.findByProyecto_IdProyectoOrderByNumeroClaseAsc(idProyecto)) {
            definiciones.put(programa.getIdClasePlataforma(), programa);
        }
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
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudieron consultar los talleres de la plataforma educativa", error);
        }
        if (remotos == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta de talleres vacía");
        Set<String> idsTaller = new HashSet<>();
        Set<Long> docentesEnLote = new HashSet<>();
        for (PlatformTallerResponse remoto : remotos) {
            if (remoto == null || remoto.idTallerPlataforma() == null || remoto.idTallerPlataforma().isBlank()
                    || !idsTaller.add(remoto.idTallerPlataforma()) || remoto.idTallerPlataforma().length() > 150
                    || !proyecto.getNombre().equals(remoto.nombreProyecto())
                    || !proyecto.getAnio().equals(remoto.anioProyecto())
                    || !proyecto.getEmpresa().getNombre().equals(remoto.nombreEmpresa())
                    || remoto.usuarioPlataforma() == null || remoto.clases() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Taller inválido o duplicado en la plataforma");
            }
            DocenteProyecto asignacion = porUsuario.get(remoto.usuarioPlataforma().trim().toLowerCase(Locale.ROOT));
            if (asignacion == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El taller " + remoto.idTallerPlataforma() + " no tiene un docente asociado");
            if (!docentesEnLote.add(asignacion.getDocente().getIdDocente()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un docente tiene más de un taller");
            if (remoto.clases().size() != definiciones.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El taller " + remoto.idTallerPlataforma() + " no contiene todas las clases del programa");
            }
            Set<Long> clasesRecibidas = new HashSet<>();
            for (PlatformTallerResponse.ClasePlataforma clase : remoto.clases()) {
                validarClase(clase, definiciones, clasesRecibidas);
            }
            Optional<Taller> existente = tallerRepository.findByIdTallerPlataforma(remoto.idTallerPlataforma());
            if (existente.isPresent() && !existente.get().getAsignacion().getProyecto().getIdProyecto().equals(idProyecto)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El identificador de taller pertenece a otro proyecto");
            }
        }
        List<Taller> locales = tallerRepository.findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(idProyecto);
        for (Taller existente : locales) {
            if (!idsTaller.contains(existente.getIdTallerPlataforma())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La plataforma omitió el taller previamente sincronizado " + existente.getIdTallerPlataforma());
            }
        }
        OffsetDateTime instante = OffsetDateTime.now(ZoneOffset.UTC);
        for (PlatformTallerResponse remoto : remotos) {
            DocenteProyecto docente = porUsuario.get(remoto.usuarioPlataforma().trim().toLowerCase(Locale.ROOT));
            Optional<Taller> encontrado = tallerRepository.findByIdTallerPlataforma(remoto.idTallerPlataforma());
            Taller taller;
            if (encontrado.isPresent()) {
                taller = encontrado.get();
                taller.actualizar(docente);
            } else {
                taller = tallerRepository.save(new Taller(remoto.idTallerPlataforma(), docente));
            }
            List<ClaseTaller> anteriores = claseRepository
                    .findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(remoto.idTallerPlataforma());
            Map<Long, ClaseTaller> clasesLocales = new HashMap<>();
            for (ClaseTaller clase : anteriores) clasesLocales.put(clase.getPrograma().getIdPrograma(), clase);
            for (PlatformTallerResponse.ClasePlataforma dato : remoto.clases()) {
                Programa programa = definiciones.get(dato.idClasePlataforma());
                ClaseTaller clase = clasesLocales.get(programa.getIdPrograma());
                EstadoClaseTaller estado = "Realizada".equals(dato.estado()) ?
                        EstadoClaseTaller.REALIZADA : EstadoClaseTaller.PENDIENTE;
                if (clase == null) {
                    clase = new ClaseTaller(taller, programa, dato.fechaRealizacion(),
                            dato.cantidadParticipantes(), dato.cantidadEvidencias(), estado, instante);
                } else {
                    clase.actualizar(dato.fechaRealizacion(), dato.cantidadParticipantes(),
                            dato.cantidadEvidencias(), estado, instante);
                }
                claseRepository.save(clase);
            }
            taller.registrarSincronizacionClases();
        }
        proyecto.registrarSincronizacionTalleres(remotos.size());
        proyecto.actualizarEstadoPorTalleres(!remotos.isEmpty());
        return remotos.size();
    }

    private void validarClase(PlatformTallerResponse.ClasePlataforma clase,
            Map<Long, Programa> programa, Set<Long> recibidas) {
        if (clase == null || clase.idClasePlataforma() == null || !recibidas.add(clase.idClasePlataforma())
                || !programa.containsKey(clase.idClasePlataforma())
                || clase.numeroClase() == null
                || !clase.numeroClase().equals(programa.get(clase.idClasePlataforma()).getNumeroClase())
                || clase.estado() == null || clase.cantidadParticipantes() == null
                || clase.cantidadEvidencias() == null || clase.cantidadParticipantes() < 0
                || clase.cantidadEvidencias() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Clase inválida o ajena al programa");
        }
        if ("Realizada".equals(clase.estado())) {
            if (clase.fechaRealizacion() == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La clase realizada no tiene fecha");
        } else if ("Pendiente".equals(clase.estado())) {
            if (clase.fechaRealizacion() != null || clase.cantidadParticipantes() != 0
                    || clase.cantidadEvidencias() != 0) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La clase pendiente debe tener fecha nula y contadores cero");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Estado de clase desconocido: " + clase.estado());
        }
    }

    /** Garantiza los permisos institucionales y la pertenencia del taller al proyecto. */
    @Transactional(readOnly = true)
    public Taller buscarTallerAutorizado(Long idProyecto, String idTaller, OidcUser usuarioGoogle) {
        Proyecto proyecto = buscarProyecto(idProyecto);
        verificarAcceso(proyecto, usuarioGoogle);
        if (idTaller == null || idTaller.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taller obligatorio");
        }
        Optional<Taller> encontrado = tallerRepository.findByIdTallerPlataforma(idTaller);
        if (encontrado.isEmpty() || !idProyecto.equals(
                encontrado.get().getAsignacion().getProyecto().getIdProyecto())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado en el proyecto");
        }
        return encontrado.get();
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
