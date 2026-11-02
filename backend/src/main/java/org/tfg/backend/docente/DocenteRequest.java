package org.tfg.backend.docente;

public record DocenteRequest(
        String nombres,
        String apellidos,
        String email,
        String pais,
        String provincia,
        String centroEducativo) {
}
