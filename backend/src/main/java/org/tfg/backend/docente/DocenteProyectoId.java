package org.tfg.backend.docente;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de la asociación entre docente y proyecto. */
public class DocenteProyectoId implements Serializable {
    private Long docente;
    private Long proyecto;

    public DocenteProyectoId() { }
    public DocenteProyectoId(Long docente, Long proyecto) {
        this.docente = docente;
        this.proyecto = proyecto;
    }
    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof DocenteProyectoId id)) return false;
        return Objects.equals(docente, id.docente) && Objects.equals(proyecto, id.proyecto);
    }
    @Override
    public int hashCode() { return Objects.hash(docente, proyecto); }
}
