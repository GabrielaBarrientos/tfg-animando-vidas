package org.tfg.backend.encuesta;

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
import org.tfg.backend.proyecto.Proyecto;

@Entity
@Table(name = "pregunta_proyecto")
public class PreguntaProyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta_proyecto")
    private Long idPreguntaProyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @Column(name = "id_pregunta_plataforma", nullable = false, unique = true)
    private Long idPreguntaPlataforma;

    @Column(name = "enunciado", nullable = false, columnDefinition = "text")
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPregunta tipo;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    protected PreguntaProyecto() { }

    public PreguntaProyecto(Proyecto proyecto, Long idPreguntaPlataforma,
            String enunciado, TipoPregunta tipo, Integer orden) {
        this.proyecto = proyecto;
        this.idPreguntaPlataforma = idPreguntaPlataforma;
        actualizar(enunciado, tipo, orden);
    }

    public void actualizar(String enunciado, TipoPregunta tipo, Integer orden) {
        this.enunciado = enunciado;
        this.tipo = tipo;
        this.orden = orden;
    }

    public Long getIdPreguntaProyecto() { return idPreguntaProyecto; }
    public Proyecto getProyecto() { return proyecto; }
    public Long getIdPreguntaPlataforma() { return idPreguntaPlataforma; }
    public String getEnunciado() { return enunciado; }
    public TipoPregunta getTipo() { return tipo; }
    public Integer getOrden() { return orden; }
}
