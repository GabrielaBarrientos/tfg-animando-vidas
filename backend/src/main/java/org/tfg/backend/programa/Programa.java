package org.tfg.backend.programa;

import java.time.OffsetDateTime;
import jakarta.persistence.*;
import org.tfg.backend.proyecto.Proyecto;

@Entity
@Table(name = "programa")
public class Programa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programa") private Long idPrograma;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false) private Proyecto proyecto;
    @Column(name = "id_clase_plataforma", nullable = false) private Long idClasePlataforma;
    @Column(name = "numero_clase", nullable = false) private Integer numeroClase;
    @Column(name = "nombre_clase", nullable = false, length = 500) private String nombreClase;
    @Column(name = "nombre_modulo", nullable = false, length = 500) private String nombreModulo;
    @Column(name = "fecha_primera_syn", nullable = false) private OffsetDateTime fechaPrimeraSyn;
    @Column(name = "fecha_ultima_syn", nullable = false) private OffsetDateTime fechaUltimaSyn;
    protected Programa() { }
    public Programa(Proyecto proyecto, Long idClasePlataforma, Integer numeroClase,
            String nombreClase, String nombreModulo, OffsetDateTime instante) {
        this.proyecto = proyecto;
        this.idClasePlataforma = idClasePlataforma;
        this.fechaPrimeraSyn = instante;
        actualizar(numeroClase, nombreClase, nombreModulo, instante);
    }
    public void actualizar(Integer numeroClase, String nombreClase, String nombreModulo, OffsetDateTime instante) {
        this.numeroClase = numeroClase;
        this.nombreClase = nombreClase;
        this.nombreModulo = nombreModulo;
        this.fechaUltimaSyn = instante;
    }
    public Long getIdPrograma() { return idPrograma; }
    public Proyecto getProyecto() { return proyecto; }
    public Long getIdClasePlataforma() { return idClasePlataforma; }
    public Integer getNumeroClase() { return numeroClase; }
    public String getNombreClase() { return nombreClase; }
    public String getNombreModulo() { return nombreModulo; }
    public OffsetDateTime getFechaPrimeraSyn() { return fechaPrimeraSyn; }
    public OffsetDateTime getFechaUltimaSyn() { return fechaUltimaSyn; }
}
