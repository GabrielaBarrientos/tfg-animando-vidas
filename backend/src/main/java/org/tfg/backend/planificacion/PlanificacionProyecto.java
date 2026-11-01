
package org.tfg.backend.planificacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

import org.tfg.backend.proyecto.Proyecto;

@Entity
@Table(name = "planificacion_proyecto")
public class PlanificacionProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planificacion_proyecto")
    private Long idPlanificacionProyecto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_proyecto",
            nullable = false,
            unique = true,
            updatable = false)
    private Proyecto proyecto;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_finalizacion", nullable = false)
    private LocalDate fechaFinalizacion;

    @Column(name = "cantidad_clases", nullable = false)
    private Integer cantidadClases;

    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia_clases", length = 20, nullable = false)
    private FrecuenciaClases frecuenciaClases;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected PlanificacionProyecto() {
        // Constructor requerido por JPA
    }

    public PlanificacionProyecto(
            Proyecto proyecto,
            LocalDate fechaInicio,
            LocalDate fechaFinalizacion,
            Integer cantidadClases,
            FrecuenciaClases frecuenciaClases) {

        this.proyecto = proyecto;
        this.fechaInicio = fechaInicio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.cantidadClases = cantidadClases;
        this.frecuenciaClases = frecuenciaClases;
    }

    public void actualizarDatos(
            LocalDate fechaInicio,
            LocalDate fechaFinalizacion,
            Integer cantidadClases,
            FrecuenciaClases frecuenciaClases) {

        this.fechaInicio = fechaInicio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.cantidadClases = cantidadClases;
        this.frecuenciaClases = frecuenciaClases;
    }

    @PrePersist
    protected void antesDeCrear() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    protected void antesDeActualizar() {
        fechaActualizacion = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getIdPlanificacionProyecto() {
        return idPlanificacionProyecto;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public Integer getCantidadClases() {
        return cantidadClases;
    }

    public FrecuenciaClases getFrecuenciaClases() {
        return frecuenciaClases;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}