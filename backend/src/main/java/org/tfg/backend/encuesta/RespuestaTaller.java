package org.tfg.backend.encuesta;

import java.time.OffsetDateTime;
import jakarta.persistence.*;
import org.tfg.backend.taller.ClaseTaller;

@Entity
@Table(name = "respuesta_taller")
public class RespuestaTaller {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta_taller") private Long idRespuestaTaller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clase_taller", nullable = false) private ClaseTaller clase;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pregunta_proyecto", nullable = false) private PreguntaProyecto pregunta;
    @Column(name = "valor_respuesta", nullable = false, columnDefinition = "text") private String valorRespuesta;
    @Column(name = "fecha_primera_sincronizacion", nullable = false) private OffsetDateTime fechaPrimeraSincronizacion;
    @Column(name = "fecha_ultima_sincronizacion", nullable = false) private OffsetDateTime fechaUltimaSincronizacion;
    protected RespuestaTaller() { }
    public RespuestaTaller(ClaseTaller clase, PreguntaProyecto pregunta,
            String valor, OffsetDateTime instante) {
        this.clase = clase;
        this.pregunta = pregunta;
        this.fechaPrimeraSincronizacion = instante;
        actualizar(valor, instante);
    }
    public void actualizar(String valor, OffsetDateTime instante) {
        valorRespuesta = valor;
        fechaUltimaSincronizacion = instante;
    }
    public ClaseTaller getClase() { return clase; }
    public PreguntaProyecto getPregunta() { return pregunta; }
    public String getValorRespuesta() { return valorRespuesta; }
}
