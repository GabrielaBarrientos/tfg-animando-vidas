
package org.tfg.backend.proyecto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

import org.tfg.backend.empresa.Empresa;

@Entity
@Table(name = "proyecto")
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proyecto")
    private Long idProyecto;

    @Column(name = "nombre", length = 200, nullable = false, updatable = false)
    private String nombre;

    @Column(name = "anio", nullable = false, updatable = false)
    private Short anio;

    @Column(name = "descripcion", columnDefinition = "text", updatable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false, updatable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    private EstadoProyecto estado;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Proyecto() {
        // Constructor requerido por JPA
    }

    public Proyecto(
            String nombre,
            Short anio,
            String descripcion,
            Empresa empresa) {

        this.nombre = nombre;
        this.anio = anio;
        this.descripcion = descripcion;
        this.empresa = empresa;
        this.estado = EstadoProyecto.PENDIENTE_PLANIFICACION;
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

    public Long getIdProyecto() {
        return idProyecto;
    }

    public String getNombre() {
        return nombre;
    }

    public Short getAnio() {
        return anio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void marcarPlanificado() {
        if (estado == EstadoProyecto.PENDIENTE_PLANIFICACION) {
            estado = EstadoProyecto.PLANIFICADO;
        }
    }

    public EstadoProyecto getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}