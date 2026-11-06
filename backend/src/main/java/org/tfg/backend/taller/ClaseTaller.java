package org.tfg.backend.taller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import org.tfg.backend.programa.Programa;

@Entity
@Table(name = "clase_taller")
public class ClaseTaller {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clase_taller") private Long idClaseTaller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_taller", nullable = false) private Taller taller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_programa", nullable = false) private Programa programa;
    @Column(name = "id_clase_plataforma", nullable = false) private Long idClasePlataforma;
    @Column(name = "numero_clase", nullable = false) private Integer numeroClase;
    @Column(name = "fecha_realizacion") private LocalDate fechaRealizacion;
    @Column(name = "cantidad_participantes", nullable = false) private Integer cantidadParticipantes;
    @Column(name = "cantidad_evidencias", nullable = false) private Integer cantidadEvidencias;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_clase", nullable = false, length = 20) private EstadoClaseTaller estadoClase;
    @Column(name = "fecha_primera_sincronizacion", nullable = false) private OffsetDateTime fechaPrimeraSincronizacion;
    @Column(name = "fecha_ultima_sincronizacion", nullable = false) private OffsetDateTime fechaUltimaSincronizacion;

    protected ClaseTaller() { }
    public ClaseTaller(Taller taller, Programa programa, LocalDate fecha,
            Integer participantes, Integer evidencias, EstadoClaseTaller estado, OffsetDateTime instante) {
        this.taller = taller;
        this.programa = programa;
        this.idClasePlataforma = programa.getIdClasePlataforma();
        this.numeroClase = programa.getNumeroClase();
        this.fechaPrimeraSincronizacion = instante;
        actualizar(fecha, participantes, evidencias, estado, instante);
    }
    public void actualizar(LocalDate fecha, Integer participantes, Integer evidencias,
            EstadoClaseTaller estado, OffsetDateTime instante) {
        this.fechaRealizacion = fecha;
        this.cantidadParticipantes = participantes;
        this.cantidadEvidencias = evidencias;
        this.estadoClase = estado;
        this.fechaUltimaSincronizacion = instante;
    }
    public Long getIdClaseTaller() { return idClaseTaller; }
    public Taller getTaller() { return taller; }
    public Programa getPrograma() { return programa; }
    public Long getIdClasePlataforma() { return idClasePlataforma; }
    public Integer getNumeroClase() { return numeroClase; }
    public String getNombreClase() { return programa.getNombreClase(); }
    public String getNombreModulo() { return programa.getNombreModulo(); }
    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public Integer getCantidadParticipantes() { return cantidadParticipantes; }
    public Integer getCantidadEvidencias() { return cantidadEvidencias; }
    public EstadoClaseTaller getEstadoClase() { return estadoClase; }
    public OffsetDateTime getFechaPrimeraSincronizacion() { return fechaPrimeraSincronizacion; }
    public OffsetDateTime getFechaUltimaSincronizacion() { return fechaUltimaSincronizacion; }
}
