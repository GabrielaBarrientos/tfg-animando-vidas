package org.tfg.backend.comunicacion;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.usuario.Usuario;

@Entity
@Table(name = "comunicacion_taller")
public class ComunicacionTaller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comunicacion")
    private Long idComunicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_taller", nullable = false, updatable = false)
    private Taller taller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_registro", nullable = false, updatable = false)
    private Usuario usuarioRegistro;

    @Column(name = "fecha_contacto", nullable = false, updatable = false)
    private LocalDate fechaContacto;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, updatable = false, length = 20)
    private CanalComunicacion canal;
    @Column(name = "canal_otro", updatable = false, length = 20)
    private String canalOtro;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, updatable = false, length = 40)
    private MotivoComunicacion motivo;
    @Column(name = "motivo_otro", updatable = false, length = 20)
    private String motivoOtro;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion_realizada", nullable = false, updatable = false, length = 20)
    private AccionComunicacion accionRealizada;
    @Column(name = "observaciones", columnDefinition = "text", updatable = false)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 40)
    private ResultadoComunicacion resultado;
    @Column(name = "fecha_respuesta_docente")
    private LocalDate fechaRespuestaDocente;
    @Column(name = "respuesta_docente", columnDefinition = "text")
    private String respuestaDocente;
    @Column(name = "fecha_compromiso")
    private LocalDate fechaCompromiso;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaUltimaModificacion;

    protected ComunicacionTaller() { }

    public ComunicacionTaller(Taller taller, Usuario usuario, CrearComunicacionRequest datos,
            String canalOtroNormalizado, String motivoOtroNormalizado, String observacionesNormalizadas) {
        this.taller = taller;
        this.usuarioRegistro = usuario;
        this.fechaContacto = datos.fechaContacto();
        this.canal = datos.canal();
        this.canalOtro = canalOtroNormalizado;
        this.motivo = datos.motivo();
        this.motivoOtro = motivoOtroNormalizado;
        this.accionRealizada = datos.accionRealizada();
        this.resultado = datos.resultado();
        this.fechaCompromiso = datos.fechaCompromiso();
        this.observaciones = observacionesNormalizadas;
    }

    /** Solo son editables los atributos del seguimiento; el contacto original queda inmutable. */
    public void actualizarSeguimiento(ActualizarSeguimientoRequest datos, String respuestaNormalizada) {
        resultado = datos.resultado();
        fechaRespuestaDocente = datos.fechaRespuestaDocente();
        respuestaDocente = respuestaNormalizada;
        fechaCompromiso = datos.fechaCompromiso();
    }

    @PrePersist
    protected void antesDeCrear() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        fechaCreacion = ahora;
        fechaUltimaModificacion = ahora;
    }

    @PreUpdate
    protected void antesDeActualizar() {
        fechaUltimaModificacion = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getIdComunicacion() { return idComunicacion; }
    public Taller getTaller() { return taller; }
    public Usuario getUsuarioRegistro() { return usuarioRegistro; }
    public LocalDate getFechaContacto() { return fechaContacto; }
    public CanalComunicacion getCanal() { return canal; }
    public String getCanalOtro() { return canalOtro; }
    public MotivoComunicacion getMotivo() { return motivo; }
    public String getMotivoOtro() { return motivoOtro; }
    public AccionComunicacion getAccionRealizada() { return accionRealizada; }
    public String getObservaciones() { return observaciones; }
    public ResultadoComunicacion getResultado() { return resultado; }
    public LocalDate getFechaRespuestaDocente() { return fechaRespuestaDocente; }
    public String getRespuestaDocente() { return respuestaDocente; }
    public LocalDate getFechaCompromiso() { return fechaCompromiso; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public OffsetDateTime getFechaUltimaModificacion() { return fechaUltimaModificacion; }
}
