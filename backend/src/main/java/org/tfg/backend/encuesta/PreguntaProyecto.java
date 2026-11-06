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
import org.tfg.backend.programa.Programa;

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

    @Column(name = "id_pregunta_plataforma", nullable = false)
    private Long idPreguntaPlataforma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_programa", nullable = false)
    private Programa programa;

    @Column(name = "enunciado", nullable = false, columnDefinition = "text")
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPregunta tipo;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    protected PreguntaProyecto() { }

    public PreguntaProyecto(Proyecto proyecto, Programa programa, Long idPreguntaPlataforma,
            String enunciado, TipoPregunta tipo, Integer orden) {
        this.proyecto = proyecto;
        this.programa = programa;
        this.idPreguntaPlataforma = idPreguntaPlataforma;
        actualizar(enunciado, tipo, orden);
    }

    public void actualizar(String enunciado, TipoPregunta tipo, Integer orden) {
        this.enunciado = enunciado;
        this.tipo = tipo;
        this.orden = orden;
    }

    public Long getIdPreguntaProyecto() { return idPreguntaProyecto; }
    public Programa getPrograma() { return programa; }
    public Proyecto getProyecto() { return proyecto; }
    public Long getIdPreguntaPlataforma() { return idPreguntaPlataforma; }
    public String getEnunciado() { return enunciado; }
    public TipoPregunta getTipo() { return tipo; }
    public Integer getOrden() { return orden; }
}
