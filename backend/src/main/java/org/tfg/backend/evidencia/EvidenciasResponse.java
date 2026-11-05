package org.tfg.backend.evidencia;

import java.time.OffsetDateTime;
import java.util.List;

public record EvidenciasResponse(OffsetDateTime ultimaSincronizacion, List<Archivo> evidencias) {
    public record Archivo(Long idEvidencia, Long idClaseTaller, int numeroClase,
            String nombreClase, String nombreFisico, String mimeType, long tamanioBytes,
            boolean esUtil, boolean esDestacada) { }
}
