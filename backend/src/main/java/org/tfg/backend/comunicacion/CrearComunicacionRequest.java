package org.tfg.backend.comunicacion;

import java.time.LocalDate;

public record CrearComunicacionRequest(
        LocalDate fechaContacto,
        CanalComunicacion canal,
        String canalOtro,
        MotivoComunicacion motivo,
        String motivoOtro,
        AccionComunicacion accionRealizada,
        ResultadoComunicacion resultado,
        LocalDate fechaCompromiso,
        String observaciones) {
}
