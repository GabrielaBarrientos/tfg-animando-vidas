package org.tfg.backend.docente;

public record DocenteResponse(
        Long idDocente,
        Long idProyecto,
        String nombres,
        String apellidos,
        String email,
        String pais,
        String provincia,
        String centroEducativo,
        boolean tieneTalleresAsociados) {
}
