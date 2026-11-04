package org.tfg.backend.taller;

public record TallerResumenResponse(
        String idTaller, String pais, String centroEducativo, String docente,
        int cantidadAlumnos, int avanceEsperado, int avanceReal,
        int diferencia, EstadoTaller estado, EstadoEvidencias evidencias,
        boolean pendienteBaja) {
    public enum EstadoTaller { NO_INICIADO, EN_PLAZO, PROXIMO_ATRASO, ATRASADO }
    public enum EstadoEvidencias { SIN_CLASES_REALIZADAS, COMPLETAS, PENDIENTES }
}
