package org.tfg.backend.comunicacion;

import java.time.LocalDate;

public record ActualizarSeguimientoRequest(
        ResultadoComunicacion resultado,
        LocalDate fechaRespuestaDocente,
        String respuestaDocente,
        LocalDate fechaCompromiso) {
}
