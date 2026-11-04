package org.tfg.backend.docente;

public record DocenteRequest(
        String nombres, String apellidos, String email, String telefono,
        String pais, String provincia, String centroEducativo,
        String cargo, String grado, String materia,
        Integer cargaHoraria, Integer cantidadAlumnos,
        String usuarioPlataforma) {
}
