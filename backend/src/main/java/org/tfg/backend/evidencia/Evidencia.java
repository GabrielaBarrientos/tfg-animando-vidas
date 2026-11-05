package org.tfg.backend.evidencia;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.tfg.backend.taller.ClaseTaller;

@Entity
@Table(name = "evidencia")
public class Evidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evidencia")
    private Long idEvidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clase_taller", nullable = false)
    private ClaseTaller clase;

    @Column(name = "id_archivo_plataforma", nullable = false, unique = true)
    private Long idArchivoPlataforma;

    @Column(name = "nombre_fisico", nullable = false, length = 500)
    private String nombreFisico;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;

    @Column(name = "es_util", nullable = false)
    private boolean esUtil;

    @Column(name = "es_destacada", nullable = false)
    private boolean esDestacada;

    @Column(name = "fecha_primera_sincronizacion", nullable = false)
    private OffsetDateTime fechaPrimeraSincronizacion;

    @Column(name = "fecha_ultima_sincronizacion", nullable = false)
    private OffsetDateTime fechaUltimaSincronizacion;

    protected Evidencia() {
        // Requerido por JPA.
    }

    public Evidencia(ClaseTaller clase, Long idExterno, String nombre, String tipo,
            Long tamanio, OffsetDateTime instante) {
        this.clase = clase;
        this.idArchivoPlataforma = idExterno;
        this.fechaPrimeraSincronizacion = instante;
        actualizar(nombre, tipo, tamanio, instante);
    }

    public void actualizar(String nombre, String tipo, Long tamanio, OffsetDateTime instante) {
        this.nombreFisico = nombre;
        this.mimeType = tipo;
        this.tamanioBytes = tamanio;
        this.fechaUltimaSincronizacion = instante;
    }

    public void marcar(boolean util, boolean destacada) {
        this.esUtil = util;
        this.esDestacada = destacada;
    }

    public Long getIdEvidencia() {
        return idEvidencia;
    }

    public ClaseTaller getClase() {
        return clase;
    }

    public Long getIdArchivoPlataforma() {
        return idArchivoPlataforma;
    }

    public String getNombreFisico() {
        return nombreFisico;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getTamanioBytes() {
        return tamanioBytes;
    }

    public boolean isEsUtil() {
        return esUtil;
    }

    public boolean isEsDestacada() {
        return esDestacada;
    }

    public OffsetDateTime getFechaPrimeraSincronizacion() {
        return fechaPrimeraSincronizacion;
    }

    public OffsetDateTime getFechaUltimaSincronizacion() {
        return fechaUltimaSincronizacion;
    }
}
