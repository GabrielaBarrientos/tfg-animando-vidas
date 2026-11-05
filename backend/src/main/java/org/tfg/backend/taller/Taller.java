package org.tfg.backend.taller;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.tfg.backend.docente.DocenteProyecto;

@Entity
@Table(name = "taller")
public class Taller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_taller")
    private Long idTaller;
    @Column(name = "id_taller_plataforma", nullable = false, unique = true, length = 150)
    private String idTallerPlataforma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_docente", referencedColumnName = "id_docente", nullable = false),
        @JoinColumn(name = "id_proyecto", referencedColumnName = "id_proyecto", nullable = false)
    })
    private DocenteProyecto asignacion;

    @Column(name = "fecha_sincronizacion", nullable = false)
    private OffsetDateTime fechaSincronizacion;
    @Column(name = "pendiente_baja", nullable = false)
    private boolean pendienteBaja;

    @Column(name = "ultima_sincronizacion_clases")
    private OffsetDateTime ultimaSincronizacionClases;

    @Column(name = "ultima_sincronizacion_evidencias")
    private OffsetDateTime ultimaSincronizacionEvidencias;

    protected Taller() { }
    public Taller(String identificador, DocenteProyecto asignacion) {
        idTallerPlataforma = identificador;
        actualizar(asignacion);
    }
    public void actualizar(DocenteProyecto asignacion) {
        this.asignacion = asignacion;
        fechaSincronizacion = OffsetDateTime.now(ZoneOffset.UTC);
        pendienteBaja = false;
    }
    public void marcarPendienteBaja() { pendienteBaja = true; }
    public void registrarSincronizacionClases() {
        ultimaSincronizacionClases = OffsetDateTime.now(ZoneOffset.UTC);
    }
    public void registrarSincronizacionEvidencias(OffsetDateTime instante) {
        ultimaSincronizacionEvidencias = instante;
    }
    public OffsetDateTime getUltimaSincronizacionEvidencias() { return ultimaSincronizacionEvidencias; }
    public Long getIdTaller() { return idTaller; }
    public String getIdTallerPlataforma() { return idTallerPlataforma; }
    public DocenteProyecto getAsignacion() { return asignacion; }
    public boolean isPendienteBaja() { return pendienteBaja; }
    public OffsetDateTime getFechaSincronizacion() { return fechaSincronizacion; }
    public OffsetDateTime getUltimaSincronizacionClases() { return ultimaSincronizacionClases; }
}
