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
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

@Service
public class EncuestasService {
    private final TallerService talleres;
    private final ClaseTallerRepository clases;
    private final PreguntaProyectoRepository preguntas;
    private final RespuestaTallerRepository respuestas;
    private final EducationalPlatformClient plataforma;

    public EncuestasService(TallerService talleres, ClaseTallerRepository clases,
            PreguntaProyectoRepository preguntas, RespuestaTallerRepository respuestas,
            EducationalPlatformClient plataforma) {
        this.talleres = talleres;
        this.clases = clases;
        this.preguntas = preguntas;
        this.respuestas = respuestas;
        this.plataforma = plataforma;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public EncuestasTallerResponse consultar(Long idProyecto, String idTaller, OidcUser usuario) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, usuario);
        List<ClaseTaller> clasesLocales = clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller);
        Map<Long, List<EncuestasTallerResponse.PreguntaRespuesta>> porClase = new HashMap<>();
        for (RespuestaTaller respuesta : respuestas.buscarPorTaller(taller.getIdTaller())) {
            PreguntaProyecto pregunta = respuesta.getPregunta();
            if (pregunta.getTipo() != TipoPregunta.CLASE) continue;
            Long idClase = respuesta.getClase().getIdClaseTaller();
            List<EncuestasTallerResponse.PreguntaRespuesta> items = porClase.get(idClase);
            if (items == null) { items = new ArrayList<>(); porClase.put(idClase, items); }
            items.add(new EncuestasTallerResponse.PreguntaRespuesta(pregunta.getIdPreguntaProyecto(),
                    pregunta.getEnunciado(), "clase", respuesta.getValorRespuesta()));
        }
        List<EncuestasTallerResponse.Clase> resultado = new ArrayList<>();
        for (ClaseTaller clase : clasesLocales) {
            List<EncuestasTallerResponse.PreguntaRespuesta> items = porClase.get(clase.getIdClaseTaller());
            if (items == null) items = new ArrayList<>();
            resultado.add(new EncuestasTallerResponse.Clase(clase.getIdClaseTaller(),
                    clase.getNumeroClase(), clase.getNombreClase(), clase.getEstadoClase().name(), items));
        }
        return new EncuestasTallerResponse(taller.getUltimaSincronizacionEncuestas(), resultado);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public int sincronizar(Long idProyecto, String idTaller, OidcUser usuario) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, usuario);
        Proyecto proyecto = taller.getAsignacion().getProyecto();
        if (!proyecto.isProgramaSincronizado()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Para actualizar respuestas, primero sincronizá el programa del proyecto");
        PlatformEncuestasResponse remoto;
        int idRemoto = convertirIdentificador(taller.getIdTallerPlataforma());
        try {
            remoto = plataforma.obtenerEncuestas(idRemoto);
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudieron consultar las respuestas de la plataforma", error);
        }
        if (remoto == null || remoto.idTallerPlataforma() == null
                || remoto.idTallerPlataforma() != idRemoto || remoto.respuestas() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuestas incompletas o de otro taller");
        }
        Map<Long, ClaseTaller> clasesPorId = new HashMap<>();
        for (ClaseTaller clase : clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller)) {
            clasesPorId.put(clase.getIdClasePlataforma(), clase);
        }
        Map<String, PreguntaProyecto> preguntasPorId = new HashMap<>();
        for (PreguntaProyecto pregunta : preguntas.findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(idProyecto)) {
            String clave = pregunta.getPrograma().getIdClasePlataforma() + ":" + pregunta.getIdPreguntaPlataforma();
            preguntasPorId.put(clave, pregunta);
        }
        Set<String> recibidas = new HashSet<>();
        for (PlatformEncuestasResponse.Respuesta item : remoto.respuestas()) {
            if (item == null || item.idTallerPlataforma() == null || item.idTallerPlataforma() != idRemoto
                    || item.idClasePlataforma() == null || item.idPreguntaPlataforma() == null
                    || item.respuesta() == null || item.respuesta().isBlank() || item.respuesta().length() > 20000) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La plataforma devolvió una respuesta inválida");
            }
            String clave = item.idClasePlataforma() + ":" + item.idPreguntaPlataforma();
            if (!clasesPorId.containsKey(item.idClasePlataforma()) || !preguntasPorId.containsKey(clave)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La respuesta " + clave + " no corresponde a una clase/pregunta sincronizada del programa");
            }
            if (!recibidas.add(clave)) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió una respuesta duplicada: " + clave);
        }
        Map<String, RespuestaTaller> anteriores = new HashMap<>();
        for (RespuestaTaller existente : respuestas.buscarPorTaller(taller.getIdTaller())) {
            String clave = existente.getClase().getIdClasePlataforma() + ":" + existente.getPregunta().getIdPreguntaPlataforma();
            anteriores.put(clave, existente);
        }
        OffsetDateTime instante = OffsetDateTime.now(ZoneOffset.UTC);
        for (PlatformEncuestasResponse.Respuesta item : remoto.respuestas()) {
            String clave = item.idClasePlataforma() + ":" + item.idPreguntaPlataforma();
            RespuestaTaller respuesta = anteriores.get(clave);
            if (respuesta == null) {
                respuesta = new RespuestaTaller(clasesPorId.get(item.idClasePlataforma()),
                        preguntasPorId.get(clave), item.respuesta(), instante);
            } else {
                respuesta.actualizar(item.respuesta(), instante);
            }
            respuestas.save(respuesta);
        }
        taller.registrarSincronizacionEncuestas(instante);
        return remoto.respuestas().size();
    }

    private int convertirIdentificador(String externo) {
        try {
            int resultado = Integer.parseInt(externo);
            if (resultado > 0) return resultado;
        } catch (NumberFormatException error) {
            // El contrato externo exige un identificador numérico.
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificador externo de taller inválido");
    }
}
