package org.tfg.backend.docente;

public record DocenteResponse(
        Long idDocente, Long idProyecto, String nombres, String apellidos, String email,
        String telefono, String pais, String provincia, String centroEducativo,
        String cargo, String grado, String materia, int cargaHoraria, int cantidadAlumnos,
        String usuarioPlataforma, boolean tieneTalleresAsociados) {
}
