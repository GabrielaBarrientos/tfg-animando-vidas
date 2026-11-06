package org.tfg.backend.encuesta;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.tfg.backend.taller.ClaseTaller;

@Entity
@Table(name = "encuesta_taller")
public class EncuestaTaller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_encuesta_taller")
    private Long idEncuestaTaller;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clase_taller", nullable = false, unique = true)
    private ClaseTaller clase;

    @Column(name = "fecha_primera_sincronizacion", nullable = false)
    private OffsetDateTime fechaPrimeraSincronizacion;

    @Column(name = "fecha_ultima_sincronizacion", nullable = false)
    private OffsetDateTime fechaUltimaSincronizacion;

    protected EncuestaTaller() { }

    public EncuestaTaller(ClaseTaller clase, OffsetDateTime instante) {
        this.clase = clase;
        this.fechaPrimeraSincronizacion = instante;
        this.fechaUltimaSincronizacion = instante;
    }

    public void registrarSincronizacion(OffsetDateTime instante) {
        this.fechaUltimaSincronizacion = instante;
    }

    public Long getIdEncuestaTaller() { return idEncuestaTaller; }
    public ClaseTaller getClase() { return clase; }
}
