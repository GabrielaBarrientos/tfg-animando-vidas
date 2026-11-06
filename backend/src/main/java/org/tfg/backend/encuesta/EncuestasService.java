package org.tfg.backend.encuesta;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformEncuestasResponse;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

@Service
public class EncuestasService {
    private final TallerService talleres;
    private final ClaseTallerRepository clases;
    private final PreguntaProyectoRepository preguntas;
    private final EncuestaTallerRepository encuestas;
    private final RespuestaEncuestaRepository respuestas;
    private final EducationalPlatformClient plataforma;

    public EncuestasService(TallerService talleres, ClaseTallerRepository clases,
            PreguntaProyectoRepository preguntas, EncuestaTallerRepository encuestas,
            RespuestaEncuestaRepository respuestas, EducationalPlatformClient plataforma) {
        this.talleres = talleres;
        this.clases = clases;
        this.preguntas = preguntas;
        this.encuestas = encuestas;
        this.respuestas = respuestas;
        this.plataforma = plataforma;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public EncuestasTallerResponse consultar(Long idProyecto, String idTaller, OidcUser usuario) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, usuario);
        List<ClaseTaller> clasesLocales = clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller);
        List<RespuestaEncuesta> respuestasLocales = respuestas.buscarPorTaller(taller.getIdTaller());
        Map<Long, List<EncuestasTallerResponse.PreguntaRespuesta>> porClase = new HashMap<>();
        for (RespuestaEncuesta respuesta : respuestasLocales) {
            Long idClase = respuesta.getEncuesta().getClase().getIdClaseTaller();
            List<EncuestasTallerResponse.PreguntaRespuesta> items = porClase.get(idClase);
            if (items == null) {
                items = new ArrayList<>();
                porClase.put(idClase, items);
            }
            PreguntaProyecto pregunta = respuesta.getPregunta();
            // Las introducciones se conservan para el análisis con IA, pero no se muestran aquí.
            if (pregunta.getTipo() == TipoPregunta.CLASE) {
                items.add(new EncuestasTallerResponse.PreguntaRespuesta(pregunta.getIdPreguntaProyecto(),
                        pregunta.getEnunciado(), "clase", respuesta.getValorRespuesta()));
            }
        }
        List<EncuestasTallerResponse.Clase> resultado = new ArrayList<>();
        for (ClaseTaller clase : clasesLocales) {
            List<EncuestasTallerResponse.PreguntaRespuesta> items = porClase.get(clase.getIdClaseTaller());
            if (items == null) {
                items = new ArrayList<>();
            }
            resultado.add(new EncuestasTallerResponse.Clase(clase.getIdClaseTaller(),
                    clase.getNumeroClase(), clase.getNombreClase(), clase.getEstadoClase().name(), items));
        }
        return new EncuestasTallerResponse(taller.getUltimaSincronizacionEncuestas(), resultado);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public int sincronizar(Long idProyecto, String idTaller, OidcUser usuario) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, usuario);
        if (taller.isPendienteBaja()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El taller está pendiente de baja");
        }
        int idRemoto = convertirIdentificador(taller.getIdTallerPlataforma());
        PlatformEncuestasResponse remoto;
        try {
            remoto = plataforma.obtenerEncuestas(idRemoto);
        } catch (Exception excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudieron consultar las encuestas de la plataforma educativa", excepcion);
        }
        if (remoto == null || remoto.idTallerPlataforma() == null || remoto.idTallerPlataforma() != idRemoto
                || remoto.respuestas() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió encuestas incompletas o de otro taller");
        }
        Map<Long, ClaseTaller> clasesPorExterno = indexarClases(idTaller);
        Map<Long, PreguntaProyecto> preguntasPorExterno = indexarPreguntas(idProyecto);
        validarRespuestas(remoto, clasesPorExterno, preguntasPorExterno);

        OffsetDateTime instante = OffsetDateTime.now(ZoneOffset.UTC);
        Map<Long, EncuestaTaller> encuestasPorClase = new HashMap<>();
        for (EncuestaTaller encuesta : encuestas.findByClase_Taller_IdTaller(taller.getIdTaller())) {
            encuestasPorClase.put(encuesta.getClase().getIdClaseTaller(), encuesta);
        }
        Map<String, RespuestaEncuesta> respuestasAnteriores = new HashMap<>();
        for (RespuestaEncuesta respuesta : respuestas.buscarPorTaller(taller.getIdTaller())) {
            String clave = claveRespuesta(respuesta.getEncuesta().getClase().getIdClasePlataforma(),
                    respuesta.getPregunta().getIdPreguntaPlataforma());
            respuestasAnteriores.put(clave, respuesta);
        }
        for (PlatformEncuestasResponse.Respuesta dato : remoto.respuestas()) {
            ClaseTaller clase = clasesPorExterno.get(dato.idClasePlataforma());
            PreguntaProyecto pregunta = preguntasPorExterno.get(dato.idPreguntaPlataforma());
            EncuestaTaller encuesta = encuestasPorClase.get(clase.getIdClaseTaller());
            if (encuesta == null) {
                encuesta = encuestas.save(new EncuestaTaller(clase, instante));
                encuestasPorClase.put(clase.getIdClaseTaller(), encuesta);
            } else {
                encuesta.registrarSincronizacion(instante);
            }
            String clave = claveRespuesta(dato.idClasePlataforma(), dato.idPreguntaPlataforma());
            RespuestaEncuesta respuesta = respuestasAnteriores.get(clave);
            if (respuesta == null) {
                respuesta = new RespuestaEncuesta(encuesta, pregunta, dato.respuesta(), instante);
            } else {
                respuesta.actualizar(dato.respuesta(), instante);
            }
            respuestas.save(respuesta);
        }
        // Una respuesta ausente no se elimina: se conserva la información ya sincronizada.
        taller.registrarSincronizacionEncuestas(instante);
        return remoto.respuestas().size();
    }

    private Map<Long, ClaseTaller> indexarClases(String idTaller) {
        Map<Long, ClaseTaller> indexadas = new HashMap<>();
        for (ClaseTaller clase : clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller)) {
            if (clase.getIdClasePlataforma() != null) {
                indexadas.put(clase.getIdClasePlataforma(), clase);
            }
        }
        return indexadas;
    }

    private Map<Long, PreguntaProyecto> indexarPreguntas(Long idProyecto) {
        Map<Long, PreguntaProyecto> indexadas = new HashMap<>();
        for (PreguntaProyecto pregunta : preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(idProyecto)) {
            indexadas.put(pregunta.getIdPreguntaPlataforma(), pregunta);
        }
        return indexadas;
    }

    private void validarRespuestas(PlatformEncuestasResponse remoto, Map<Long, ClaseTaller> clases,
            Map<Long, PreguntaProyecto> preguntas) {
        Set<String> recibidas = new HashSet<>();
        for (PlatformEncuestasResponse.Respuesta dato : remoto.respuestas()) {
            if (dato == null || dato.idTallerPlataforma() == null
                    || !dato.idTallerPlataforma().equals(remoto.idTallerPlataforma())
                    || dato.idClasePlataforma() == null || dato.idClasePlataforma() <= 0
                    || dato.idPreguntaPlataforma() == null || dato.idPreguntaPlataforma() <= 0
                    || dato.respuesta() == null || dato.respuesta().isBlank()
                    || dato.respuesta().length() > 20000) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta de encuesta inválida");
            }
            if (!clases.containsKey(dato.idClasePlataforma())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Actualizá primero las clases del taller para asociar sus encuestas");
            }
            if (!preguntas.containsKey(dato.idPreguntaPlataforma())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Actualizá primero los talleres del proyecto para obtener sus preguntas");
            }
            if (!recibidas.add(claveRespuesta(dato.idClasePlataforma(), dato.idPreguntaPlataforma()))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "La plataforma devolvió dos respuestas para una misma pregunta y clase");
            }
        }
    }

    private String claveRespuesta(Long clase, Long pregunta) {
        return clase + ":" + pregunta;
    }

    private int convertirIdentificador(String externo) {
        try {
            int id = Integer.parseInt(externo);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException excepcion) {
            // No se pueden consultar encuestas con un identificador no numérico.
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El identificador externo del taller debe ser numérico");
    }
}
