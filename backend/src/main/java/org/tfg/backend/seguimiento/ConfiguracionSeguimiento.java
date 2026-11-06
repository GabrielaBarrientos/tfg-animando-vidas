package org.tfg.backend.seguimiento;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.usuario.Usuario;

@Entity
@Table(name = "configuracion_proyecto")
public class ConfiguracionSeguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion_seguimiento")
    private Long idConfiguracionSeguimiento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true, updatable = false)
    private Proyecto proyecto;

    @Column(name = "umbral_proximo_atraso_semanas", nullable = false)
    private Integer umbralProximoAtrasoSemanas;

    @Column(name = "umbral_atraso_semanas", nullable = false)
    private Integer umbralAtrasoSemanas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_responsable_seguimiento", nullable = false)
    private Usuario responsableSeguimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_informe_semanal", nullable = false, length = 12)
    private DiaInformeSemanal diaInformeSemanal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_responsable_proyecto", nullable = false)
    private Usuario responsableProyecto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dia_informe_mensual", nullable = false, length = 20)
    private TipoDiaInformeMensual tipoDiaInformeMensual;

    @Column(name = "dia_informe_mensual")
    private Integer diaInformeMensual;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected ConfiguracionSeguimiento() {
        // Requerido por JPA.
    }

    public ConfiguracionSeguimiento(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public void actualizarDatos(
            ConfiguracionSeguimientoRequest datos,
            Usuario responsableSeguimiento,
            Usuario responsableProyecto) {

        this.umbralProximoAtrasoSemanas = datos.umbralProximoAtrasoSemanas();
        this.umbralAtrasoSemanas = datos.umbralAtrasoSemanas();
        this.responsableSeguimiento = responsableSeguimiento;
        this.diaInformeSemanal = datos.diaInformeSemanal();
        this.responsableProyecto = responsableProyecto;
        this.tipoDiaInformeMensual = datos.tipoDiaInformeMensual();
        this.diaInformeMensual = datos.diaInformeMensual();
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

    public Long getIdConfiguracionSeguimiento() {
        return idConfiguracionSeguimiento;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public Integer getUmbralProximoAtrasoSemanas() {
        return umbralProximoAtrasoSemanas;
    }

    public Integer getUmbralAtrasoSemanas() {
        return umbralAtrasoSemanas;
    }

    public Usuario getResponsableSeguimiento() {
        return responsableSeguimiento;
    }

    public DiaInformeSemanal getDiaInformeSemanal() {
        return diaInformeSemanal;
    }

    public Usuario getResponsableProyecto() {
        return responsableProyecto;
    }

    public TipoDiaInformeMensual getTipoDiaInformeMensual() {
        return tipoDiaInformeMensual;
    }

    public Integer getDiaInformeMensual() {
        return diaInformeMensual;
    }
}
