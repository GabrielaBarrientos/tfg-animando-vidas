package org.tfg.backend.encuesta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.PlatformTallerResponse;
import org.tfg.backend.proyecto.Proyecto;

/** Sincroniza una sola copia de las preguntas comunes a los talleres del proyecto. */
@Service
public class PreguntasProyectoSincronizador {
    private final PreguntaProyectoRepository repositorio;

    public PreguntasProyectoSincronizador(PreguntaProyectoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Verificación previa a toda escritura de la sincronización de talleres. */
    public void validarCatalogo(List<PlatformTallerResponse> talleres) {
        Map<Integer, Map<Long, String>> referencia = null;
        for (PlatformTallerResponse taller : talleres) {
            Map<Integer, Map<Long, String>> porNumeroClase = new HashMap<>();
            Set<Long> identificadoresDelTaller = new HashSet<>();
            for (PlatformTallerResponse.ClasePlataforma clase : taller.clases()) {
                if (clase == null || clase.numeroClase() == null || clase.preguntas() == null) {
                    throw respuestaInvalida("La plataforma no informó las preguntas de una clase");
                }
                Map<Long, String> preguntasClase = new HashMap<>();
                Set<Integer> ordenes = new HashSet<>();
                for (PlatformTallerResponse.PreguntaPlataforma pregunta : clase.preguntas()) {
                    TipoPregunta tipo = validarPregunta(pregunta);
                    if (!identificadoresDelTaller.add(pregunta.idPreguntaPlataforma())) {
                        throw respuestaInvalida("Una pregunta aparece en más de una clase del mismo taller");
                    }
                    if (!ordenes.add(pregunta.orden())) {
                        throw respuestaInvalida("Dos preguntas comparten el mismo orden en una clase");
                    }
                    String firma = pregunta.enunciado().trim() + "\n" + tipo.name()
                            + "\n" + pregunta.orden();
                    preguntasClase.put(pregunta.idPreguntaPlataforma(), firma);
                }
                for (int numero = 1; numero <= clase.preguntas().size(); numero++) {
                    if (!ordenes.contains(numero)) {
                        throw respuestaInvalida("El orden de las preguntas debe comenzar en 1 y ser consecutivo");
                    }
                }
                if (porNumeroClase.put(clase.numeroClase(), preguntasClase) != null) {
                    throw respuestaInvalida("Número de clase duplicado en el catálogo");
                }
            }
            if (referencia == null) {
                referencia = porNumeroClase;
            } else if (!referencia.equals(porNumeroClase)) {
                throw respuestaInvalida("Las preguntas de cada clase deben ser iguales en todos los talleres del proyecto");
            }
        }
    }

    public void guardarCatalogo(Proyecto proyecto, List<PlatformTallerResponse> talleres) {
        if (talleres.isEmpty()) {
            return;
        }
        Map<Long, PreguntaProyecto> existentes = new HashMap<>();
        List<PreguntaProyecto> guardadas = repositorio
                .findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(proyecto.getIdProyecto());
        for (PreguntaProyecto guardada : guardadas) {
            existentes.put(guardada.getIdPreguntaPlataforma(), guardada);
        }
        Set<Long> procesadas = new HashSet<>();
        // El contrato exige el mismo catálogo por número de clase para todos los talleres.
        // Basta leer una copia, pero se recorren TODAS sus clases.
        for (PlatformTallerResponse.ClasePlataforma clase : talleres.get(0).clases()) {
            for (PlatformTallerResponse.PreguntaPlataforma remota : clase.preguntas()) {
                if (!procesadas.add(remota.idPreguntaPlataforma())) {
                    throw respuestaInvalida("Identificador de pregunta duplicado en el catálogo");
                }
                TipoPregunta tipo = validarPregunta(remota);
                PreguntaProyecto local = existentes.get(remota.idPreguntaPlataforma());
                if (local == null) {
                    local = new PreguntaProyecto(proyecto, remota.idPreguntaPlataforma(),
                            remota.enunciado().trim(), tipo, remota.orden());
                } else {
                    local.actualizar(remota.enunciado().trim(), tipo, remota.orden());
                }
                repositorio.save(local);
            }
        }
        // No se eliminan preguntas históricas: pueden tener respuestas vinculadas.
    }

    private TipoPregunta validarPregunta(PlatformTallerResponse.PreguntaPlataforma pregunta) {
        if (pregunta == null || pregunta.idPreguntaPlataforma() == null
                || pregunta.idPreguntaPlataforma() <= 0 || pregunta.enunciado() == null
                || pregunta.enunciado().isBlank() || pregunta.enunciado().length() > 10000
                || pregunta.tipo() == null || pregunta.orden() == null || pregunta.orden() <= 0) {
            throw respuestaInvalida("La plataforma devolvió una pregunta inválida");
        }
        if ("introducción".equals(pregunta.tipo())) {
            return TipoPregunta.INTRODUCCION;
        }
        if ("clase".equals(pregunta.tipo())) {
            return TipoPregunta.CLASE;
        }
        throw respuestaInvalida("Tipo de pregunta desconocido: " + pregunta.tipo());
    }

    private ResponseStatusException respuestaInvalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
