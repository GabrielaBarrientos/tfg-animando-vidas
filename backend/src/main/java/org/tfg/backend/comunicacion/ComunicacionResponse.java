package org.tfg.backend.comunicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ComunicacionResponse(
        Long idComunicacion,
        LocalDate fechaContacto,
        CanalComunicacion canal,
        String canalOtro,
        MotivoComunicacion motivo,
        String motivoOtro,
        AccionComunicacion accionRealizada,
        String observaciones,
        ResultadoComunicacion resultado,
        LocalDate fechaRespuestaDocente,
        String respuestaDocente,
        LocalDate fechaCompromiso,
        Long idUsuarioRegistro,
        String registradoPor,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaUltimaModificacion) {

    public static ComunicacionResponse desdeEntidad(ComunicacionTaller comunicacion) {
        return new ComunicacionResponse(
                comunicacion.getIdComunicacion(),
                comunicacion.getFechaContacto(),
                comunicacion.getCanal(),
                comunicacion.getCanalOtro(),
                comunicacion.getMotivo(),
                comunicacion.getMotivoOtro(),
                comunicacion.getAccionRealizada(),
                comunicacion.getObservaciones(),
                comunicacion.getResultado(),
                comunicacion.getFechaRespuestaDocente(),
                comunicacion.getRespuestaDocente(),
                comunicacion.getFechaCompromiso(),
                comunicacion.getUsuarioRegistro().getIdUsuario(),
                comunicacion.getUsuarioRegistro().getNombres() + " "
                        + comunicacion.getUsuarioRegistro().getApellidos(),
                comunicacion.getFechaCreacion(),
                comunicacion.getFechaUltimaModificacion());
    }
}
