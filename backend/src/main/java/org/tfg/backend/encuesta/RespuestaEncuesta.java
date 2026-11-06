package org.tfg.backend.encuesta;

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

@Entity
@Table(name = "respuesta_encuesta")
public class RespuestaEncuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta_encuesta")
    private Long idRespuestaEncuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_encuesta_taller", nullable = false)
    private EncuestaTaller encuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pregunta_proyecto", nullable = false)
    private PreguntaProyecto pregunta;

    @Column(name = "valor_respuesta", nullable = false, columnDefinition = "text")
    private String valorRespuesta;

    @Column(name = "fecha_primera_sincronizacion", nullable = false)
    private OffsetDateTime fechaPrimeraSincronizacion;

    @Column(name = "fecha_ultima_sincronizacion", nullable = false)
    private OffsetDateTime fechaUltimaSincronizacion;

    protected RespuestaEncuesta() { }

    public RespuestaEncuesta(EncuestaTaller encuesta, PreguntaProyecto pregunta,
            String valorRespuesta, OffsetDateTime instante) {
        this.encuesta = encuesta;
        this.pregunta = pregunta;
        this.fechaPrimeraSincronizacion = instante;
        actualizar(valorRespuesta, instante);
    }

    public void actualizar(String valor, OffsetDateTime instante) {
        this.valorRespuesta = valor;
        this.fechaUltimaSincronizacion = instante;
    }

    public EncuestaTaller getEncuesta() { return encuesta; }
    public PreguntaProyecto getPregunta() { return pregunta; }
    public String getValorRespuesta() { return valorRespuesta; }
}
