package org.tfg.backend.taller;

import java.time.OffsetDateTime;

/** Información general independiente de los filtros de la tabla. */
public record TalleresMetadatosResponse(
        String estadoProyecto,
        OffsetDateTime ultimaSincronizacion,
        int cantidadTalleresPlataforma) { }
