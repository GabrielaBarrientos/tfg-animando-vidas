package org.tfg.backend.docente;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneradorUsuarioPlataformaTests {
    private final GeneradorUsuarioPlataforma generador = new GeneradorUsuarioPlataforma();

    @Test
    void respetaEjemplosAprobados() {
        assertEquals("lara.kon", generador.generar("Lara", "Kon"));
        assertEquals("clar.kon", generador.generar("Clara", "Kon"));
        assertEquals("adaa.kon", generador.generar("Ada", "Kon"));
        assertEquals("anaan.li", generador.generar("Ana", "Li"));
        assertEquals("m.gonzalez", generador.generar("María Elena", "González Pérez"));
    }

    @Test
    void validaFormatoManual() {
        assertTrue(generador.formatoValido("maria.elena.gonzalez.perez"));
        assertFalse(generador.formatoValido("m.gar"));
        assertFalse(generador.formatoValido("maria123.perez"));
        assertFalse(generador.formatoValido("mariaperez"));
        assertFalse(generador.formatoValido("maria..perez"));
    }
}
