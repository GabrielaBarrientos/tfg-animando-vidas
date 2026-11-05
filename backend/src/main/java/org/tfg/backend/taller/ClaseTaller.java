package org.tfg.backend.taller;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clase_taller")
@IdClass(ClaseTallerId.class)
public class ClaseTaller {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_taller_plataforma", nullable = false)
    private Taller taller;
    @Id
    @Column(name = "numero_clase", nullable = false)
    private Integer numeroClase;
    @Column(name = "fecha_realizacion")
    private LocalDate fechaRealizacion;
    @Column(name = "cantidad_participantes")
    private Integer cantidadParticipantes;
    @Column(name = "cantidad_evidencias", nullable = false)
    private Integer cantidadEvidencias;

    @Column(name = "nombre_clase", length = 500)
    private String nombreClase;
    @Column(name = "nombre_modulo", length = 500)
    private String nombreModulo;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_clase", nullable = false, length = 20)
    private EstadoClaseTaller estadoClase;

    protected ClaseTaller() { }
    public ClaseTaller(Taller taller, int numeroClase, LocalDate fechaRealizacion,
            Integer cantidadParticipantes, int cantidadEvidencias) {
        this.taller = taller;
        this.numeroClase = numeroClase;
        actualizar(fechaRealizacion, cantidadParticipantes, cantidadEvidencias);
    }
    public void actualizar(LocalDate fechaRealizacion, Integer cantidadParticipantes, int cantidadEvidencias) {
        this.fechaRealizacion = fechaRealizacion;
        this.cantidadParticipantes = cantidadParticipantes;
        this.cantidadEvidencias = cantidadEvidencias;
        this.estadoClase = fechaRealizacion == null ? EstadoClaseTaller.PENDIENTE : EstadoClaseTaller.REALIZADA;
    }
    public void actualizarDetalle(String nombreClase, String nombreModulo, EstadoClaseTaller estado,
            LocalDate fechaRealizacion, Integer participantes, Integer evidencias) {
        this.nombreClase = nombreClase;
        this.nombreModulo = nombreModulo;
        this.estadoClase = estado;
        this.fechaRealizacion = fechaRealizacion;
        this.cantidadParticipantes = participantes;
        this.cantidadEvidencias = evidencias == null ? 0 : evidencias;
    }
    public Taller getTaller() { return taller; }
    public Integer getNumeroClase() { return numeroClase; }
    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public Integer getCantidadParticipantes() { return cantidadParticipantes; }
    public Integer getCantidadEvidencias() { return cantidadEvidencias; }
    public String getNombreClase() { return nombreClase; }
    public String getNombreModulo() { return nombreModulo; }
    public EstadoClaseTaller getEstadoClase() { return estadoClase; }
}
