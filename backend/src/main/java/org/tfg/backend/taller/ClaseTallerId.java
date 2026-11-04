package org.tfg.backend.taller;

import java.io.Serializable;
import java.util.Objects;

public class ClaseTallerId implements Serializable {
    private String taller;
    private Integer numeroClase;

    public ClaseTallerId() { }
    public ClaseTallerId(String taller, Integer numeroClase) {
        this.taller = taller;
        this.numeroClase = numeroClase;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof ClaseTallerId identificador)) return false;
        return Objects.equals(taller, identificador.taller)
                && Objects.equals(numeroClase, identificador.numeroClase);
    }

    @Override
    public int hashCode() { return Objects.hash(taller, numeroClase); }
}
