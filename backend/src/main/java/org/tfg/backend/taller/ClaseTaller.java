package org.tfg.backend.taller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clase_taller")
public class ClaseTaller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clase_taller")
    private Long idClaseTaller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_taller", nullable = false)
    private Taller taller;
    @Column(name = "id_clase_plataforma", unique = true)
    private Long idClasePlataforma;
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
    @Column(name = "fecha_primera_sincronizacion", nullable = false)
    private OffsetDateTime fechaPrimeraSincronizacion;
    @Column(name = "fecha_ultima_sincronizacion", nullable = false)
    private OffsetDateTime fechaUltimaSincronizacion;

    protected ClaseTaller() { }
    public ClaseTaller(Taller taller, int numeroClase, LocalDate fechaRealizacion,
            Integer participantes, int cantidadEvidencias) {
        this(taller, numeroClase, fechaRealizacion, participantes, cantidadEvidencias,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
    public ClaseTaller(Taller taller, int numeroClase, LocalDate fechaRealizacion,
            Integer participantes, int cantidadEvidencias, OffsetDateTime instante) {
        this.taller = taller;
        this.numeroClase = numeroClase;
        this.fechaPrimeraSincronizacion = instante;
        actualizar(fechaRealizacion, participantes, cantidadEvidencias, instante);
    }
    public void actualizar(LocalDate fechaRealizacion, Integer participantes, int cantidadEvidencias) {
        actualizar(fechaRealizacion, participantes, cantidadEvidencias, OffsetDateTime.now(ZoneOffset.UTC));
    }
    public void actualizar(LocalDate fechaRealizacion, Integer participantes, int cantidadEvidencias,
            OffsetDateTime instante) {
        this.fechaRealizacion = fechaRealizacion;
        this.cantidadParticipantes = participantes;
        this.cantidadEvidencias = cantidadEvidencias;
        this.estadoClase = fechaRealizacion == null ? EstadoClaseTaller.PENDIENTE : EstadoClaseTaller.REALIZADA;
        this.fechaUltimaSincronizacion = instante;
    }
    public void actualizarDetalle(String nombreClase, String nombreModulo, EstadoClaseTaller estado,
            LocalDate fechaRealizacion, Integer participantes, Integer evidencias) {
        actualizarDetalle(nombreClase, nombreModulo, estado, fechaRealizacion, participantes, evidencias,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
    public void actualizarDetalle(String nombreClase, String nombreModulo, EstadoClaseTaller estado,
            LocalDate fechaRealizacion, Integer participantes, Integer evidencias, OffsetDateTime instante) {
        this.nombreClase = nombreClase;
        this.nombreModulo = nombreModulo;
        this.estadoClase = estado;
        this.fechaRealizacion = fechaRealizacion;
        this.cantidadParticipantes = participantes;
        this.cantidadEvidencias = evidencias == null ? 0 : evidencias;
        this.fechaUltimaSincronizacion = instante;
    }
    public void asignarIdentificadorExterno(Long idExterno) {
        if (idExterno == null || idExterno <= 0) throw new IllegalArgumentException("ID de clase inválido");
        if (idClasePlataforma != null && !idClasePlataforma.equals(idExterno)) {
            throw new IllegalStateException("No se puede reasignar el identificador externo de una clase");
        }
        idClasePlataforma = idExterno;
    }
    public Long getIdClaseTaller() { return idClaseTaller; }
    public Taller getTaller() { return taller; }
    public Long getIdClasePlataforma() { return idClasePlataforma; }
    public Integer getNumeroClase() { return numeroClase; }
    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public Integer getCantidadParticipantes() { return cantidadParticipantes; }
    public Integer getCantidadEvidencias() { return cantidadEvidencias; }
    public String getNombreClase() { return nombreClase; }
    public String getNombreModulo() { return nombreModulo; }
    public EstadoClaseTaller getEstadoClase() { return estadoClase; }
    public OffsetDateTime getFechaPrimeraSincronizacion() { return fechaPrimeraSincronizacion; }
    public OffsetDateTime getFechaUltimaSincronizacion() { return fechaUltimaSincronizacion; }
}
