package org.tfg.backend.programa;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.web.client.HttpClientErrorException;
import org.tfg.backend.encuesta.PreguntaProyecto;
import org.tfg.backend.encuesta.PreguntaProyectoRepository;
import org.tfg.backend.encuesta.TipoPregunta;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformProgramaResponse;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

@Service
public class ProgramaService {
    private final ProgramaRepository programas;
    private final PreguntaProyectoRepository preguntas;
    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;
    private final EducationalPlatformClient plataforma;

    public ProgramaService(ProgramaRepository programas, PreguntaProyectoRepository preguntas,
            ProyectoRepository proyectos, UsuarioRepository usuarios, EducationalPlatformClient plataforma) {
        this.programas = programas;
        this.preguntas = preguntas;
        this.proyectos = proyectos;
        this.usuarios = usuarios;
        this.plataforma = plataforma;
    }

    public PlatformProgramaResponse consultarYValidar(String nombre, String empresa, Short anio) {
        PlatformProgramaResponse remoto;
        try {
            remoto = plataforma.obtenerPrograma(nombre, empresa, anio);
        } catch (HttpClientErrorException.NotFound error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No existe un programa para el proyecto en la plataforma educativa", error);
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible verificar el programa en la plataforma educativa", error);
        }
        validar(remoto);
        return remoto;
    }

    /** Debe invocarse en la misma transacción del alta del proyecto. */
    @Transactional
    public void guardarPrograma(Proyecto proyecto, PlatformProgramaResponse remoto) {
        validar(remoto);
        Map<Long, Programa> locales = new HashMap<>();
        for (Programa existente : programas.findByProyecto_IdProyectoOrderByNumeroClaseAsc(proyecto.getIdProyecto())) {
            locales.put(existente.getIdClasePlataforma(), existente);
        }
        Map<String, PreguntaProyecto> preguntasLocales = new HashMap<>();
        for (PreguntaProyecto existente : preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(proyecto.getIdProyecto())) {
            String clave = existente.getPrograma().getIdClasePlataforma() + ":" + existente.getIdPreguntaPlataforma();
            preguntasLocales.put(clave, existente);
        }
        OffsetDateTime instante = OffsetDateTime.now(ZoneOffset.UTC);
        Set<Long> clasesActuales = new HashSet<>();
        Set<String> preguntasActuales = new HashSet<>();
        for (PlatformProgramaResponse.ClasePrograma clase : remoto.clase()) {
            clasesActuales.add(clase.idClasePlataforma());
            Programa programa = locales.get(clase.idClasePlataforma());
            if (programa == null) {
                programa = programas.save(new Programa(proyecto, clase.idClasePlataforma(),
                        clase.numeroClase(), clase.nombreClase(), clase.nombreModulo(), instante));
            } else {
                programa.actualizar(clase.numeroClase(), clase.nombreClase(), clase.nombreModulo(), instante);
            }
            for (PlatformProgramaResponse.PreguntaPrograma item : clase.pregunta()) {
                String clave = clase.idClasePlataforma() + ":" + item.idPreguntaPlataforma();
                preguntasActuales.add(clave);
                PreguntaProyecto anterior = preguntasLocales.get(clave);
                TipoPregunta tipo = "intro".equals(item.tipo()) ? TipoPregunta.INTRODUCCION : TipoPregunta.CLASE;
                if (anterior == null) {
                    preguntas.save(new PreguntaProyecto(proyecto, programa, item.idPreguntaPlataforma(),
                            item.enunciado(), tipo, item.numeroPregunta()));
                } else {
                    anterior.actualizar(item.enunciado(), tipo, item.numeroPregunta());
                }
            }
        }
        // Los programas y preguntas nunca se eliminan silenciosamente: otros datos podrían referenciarlos.
        for (Long id : locales.keySet()) {
            if (!clasesActuales.contains(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La plataforma omitió una clase ya sincronizada: " + id);
            }
        }
        for (PreguntaProyecto local : preguntasLocales.values()) {
            String clave = local.getPrograma().getIdClasePlataforma() + ":" + local.getIdPreguntaPlataforma();
            if (!preguntasActuales.contains(clave)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La plataforma omitió una pregunta ya sincronizada: " + local.getIdPreguntaPlataforma());
            }
        }
        proyecto.registrarSincronizacionPrograma(instante);
    }

    public void validar(PlatformProgramaResponse remoto) {
        if (remoto == null || remoto.clase() == null || remoto.clase().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El programa no existe o no contiene clases");
        }
        Set<Long> idsClases = new HashSet<>();
        Set<Integer> numerosClases = new HashSet<>();
        for (PlatformProgramaResponse.ClasePrograma clase : remoto.clase()) {
            if (clase == null || clase.idClasePlataforma() == null || clase.idClasePlataforma() <= 0
                    || !idsClases.add(clase.idClasePlataforma()) || clase.numeroClase() == null
                    || clase.numeroClase() < 1 || !numerosClases.add(clase.numeroClase())
                    || clase.nombreClase() == null || clase.nombreClase().isBlank() || clase.nombreClase().length() > 500
                    || clase.nombreModulo() == null || clase.nombreModulo().isBlank() || clase.nombreModulo().length() > 500
                    || clase.pregunta() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Clase inválida en el programa");
            }
            Set<Integer> numeros = new HashSet<>();
            Set<Long> ids = new HashSet<>();
            for (PlatformProgramaResponse.PreguntaPrograma pregunta : clase.pregunta()) {
                if (pregunta == null || pregunta.idPreguntaPlataforma() == null || pregunta.idPreguntaPlataforma() <= 0
                        || !ids.add(pregunta.idPreguntaPlataforma()) || pregunta.numeroPregunta() == null
                        || pregunta.numeroPregunta() < 1 || !numeros.add(pregunta.numeroPregunta())
                        || pregunta.enunciado() == null || pregunta.enunciado().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Pregunta inválida en la clase " + clase.numeroClase());
                }
                if (!"texto".equals(pregunta.formato())) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "La pregunta " + pregunta.idPreguntaPlataforma() + " de la clase "
                                    + clase.numeroClase() + " tiene formato no admitido: " + pregunta.formato());
                }
                if (!"intro".equals(pregunta.tipo()) && !"clase".equals(pregunta.tipo())) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Tipo de pregunta no admitido: " + pregunta.tipo());
                }
            }
            for (int numero = 1; numero <= numeros.size(); numero++) {
                if (!numeros.contains(numero)) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El orden de preguntas de la clase " + clase.numeroClase() + " debe ser consecutivo");
            }
        }
        for (int numero = 1; numero <= numerosClases.size(); numero++) {
            if (!numerosClases.contains(numero)) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Los números de las clases del programa deben ser consecutivos");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public ProgramaResponse consultar(Long idProyecto, OidcUser google) {
        Proyecto proyecto = buscarAutorizado(idProyecto, google, false);
        List<ProgramaResponse.Clase> resultado = new ArrayList<>();
        for (Programa programa : programas.findByProyecto_IdProyectoOrderByNumeroClaseAsc(idProyecto)) {
            List<ProgramaResponse.Pregunta> lista = new ArrayList<>();
            for (PreguntaProyecto pregunta : preguntas.findByPrograma_IdProgramaOrderByOrdenAsc(programa.getIdPrograma())) {
                lista.add(new ProgramaResponse.Pregunta(pregunta.getIdPreguntaProyecto(),
                        pregunta.getIdPreguntaPlataforma(), pregunta.getOrden(),
                        pregunta.getTipo() == TipoPregunta.CLASE ? "clase" : "intro", pregunta.getEnunciado()));
            }
            resultado.add(new ProgramaResponse.Clase(programa.getIdPrograma(), programa.getIdClasePlataforma(),
                    programa.getNumeroClase(), programa.getNombreClase(), programa.getNombreModulo(), lista));
        }
        return new ProgramaResponse(proyecto.isProgramaSincronizado(),
                proyecto.getUltimaSincronizacionPrograma(), resultado);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public ProgramaResponse actualizar(Long idProyecto, OidcUser google) {
        Proyecto proyecto = buscarAutorizado(idProyecto, google, true);
        PlatformProgramaResponse remoto = consultarYValidar(proyecto.getNombre(), proyecto.getEmpresa().getNombre(), proyecto.getAnio());
        guardarPrograma(proyecto, remoto);
        return consultar(idProyecto, google);
    }

    private Proyecto buscarAutorizado(Long idProyecto, OidcUser google, boolean escribir) {
        if (google == null) throw new AccessDeniedException("Sesión requerida");
        Optional<Proyecto> proyectoEncontrado = proyectos.findById(idProyecto);
        if (proyectoEncontrado.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        Proyecto proyecto = proyectoEncontrado.get();
        Optional<Usuario> encontrado = usuarios.findByGoogleSub(google.getSubject());
        if (encontrado.isEmpty() || !encontrado.get().isHabilitado()) throw new AccessDeniedException("Acceso denegado");
        Usuario usuario = encontrado.get();
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) return proyecto;
        if (!escribir && usuario.getRol() == RolUsuario.EXTERNO
                && proyecto.getEmpresa().getIdEmpresa().equals(usuario.getIdEmpresa())) return proyecto;
        if (usuario.getRol() == RolUsuario.OPERATIVO) {
            for (Proyecto asignado : usuario.getProyectosAsignados()) {
                if (idProyecto.equals(asignado.getIdProyecto())) return proyecto;
            }
        }
        throw new AccessDeniedException("No tenés acceso a este proyecto");
    }
}
