package org.tfg.backend.evidencia;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Streaming sin mantener el contenido íntegro en memoria. */
public class TransferenciaBinaria implements StreamingResponseBody {
    private final InputStream entrada;
    private final Path archivoTemporal;

    public TransferenciaBinaria(InputStream entrada) {
        this.entrada = entrada;
        this.archivoTemporal = null;
    }

    public TransferenciaBinaria(Path archivoTemporal) {
        this.archivoTemporal = archivoTemporal;
        this.entrada = null;
    }

    @Override
    public void writeTo(OutputStream salida) throws IOException {
        try {
            if (archivoTemporal != null) {
                try (InputStream origen = Files.newInputStream(archivoTemporal)) {
                    copiar(origen, salida);
                }
            } else {
                try (InputStream origen = entrada) {
                    copiar(origen, salida);
                }
            }
        } finally {
            if (archivoTemporal != null) {
                Files.deleteIfExists(archivoTemporal);
            }
        }
    }

    private void copiar(InputStream origen, OutputStream salida) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        int cantidad;
        while ((cantidad = origen.read(buffer)) != -1) {
            salida.write(buffer, 0, cantidad);
        }
        salida.flush();
    }
}
