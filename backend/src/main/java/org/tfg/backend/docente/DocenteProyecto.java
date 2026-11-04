package org.tfg.backend.docente;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.tfg.backend.proyecto.Proyecto;

/** Atributos propios de la participación de una persona en un proyecto. */
@Entity
@IdClass(DocenteProyectoId.class)
@Table(name = "docente_proyecto")
public class DocenteProyecto {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_docente", nullable = false, updatable = false)
    private Docente docente;
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, updatable = false)
    private Proyecto proyecto;

    @Column(name = "usuario_plataforma", nullable = false, length = 150)
    private String usuarioPlataforma;
    @Column(name = "pais", nullable = false, length = 100)
    private String pais;
    @Column(name = "provincia", nullable = false, length = 150)
    private String provincia;
    @Column(name = "centro_educativo", nullable = false, length = 200)
    private String centroEducativo;
    @Column(name = "cargo", nullable = false, length = 150)
    private String cargo;
    @Column(name = "grado", nullable = false, length = 100)
    private String grado;
    @Column(name = "materia", nullable = false, length = 150)
    private String materia;
    @Column(name = "carga_horaria", nullable = false)
    private Integer cargaHoraria;
    @Column(name = "cantidad_alumnos", nullable = false)
    private Integer cantidadAlumnos;
    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected DocenteProyecto() { }
    public DocenteProyecto(Docente docente, Proyecto proyecto, DocenteRequest datos, String usuario) {
        this.docente = docente;
        this.proyecto = proyecto;
        actualizarDatos(datos, usuario);
    }
    public void actualizarDatos(DocenteRequest datos, String usuario) {
        usuarioPlataforma = usuario.toLowerCase(Locale.ROOT).trim();
        pais = datos.pais().trim();
        provincia = datos.provincia().trim();
        centroEducativo = datos.centroEducativo().trim();
        cargo = datos.cargo().trim();
        grado = datos.grado().trim();
        materia = datos.materia().trim();
        cargaHoraria = datos.cargaHoraria();
        cantidadAlumnos = datos.cantidadAlumnos();
    }
    @PrePersist
    protected void antesDeCrear() {
        fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC);
        fechaActualizacion = fechaCreacion;
    }
    @PreUpdate
    protected void antesDeActualizar() { fechaActualizacion = OffsetDateTime.now(ZoneOffset.UTC); }

    public Docente getDocente() { return docente; }
    public Proyecto getProyecto() { return proyecto; }
    public String getUsuarioPlataforma() { return usuarioPlataforma; }
    public String getPais() { return pais; }
    public String getProvincia() { return provincia; }
    public String getCentroEducativo() { return centroEducativo; }
    public String getCargo() { return cargo; }
    public String getGrado() { return grado; }
    public String getMateria() { return materia; }
    public Integer getCargaHoraria() { return cargaHoraria; }
    public Integer getCantidadAlumnos() { return cantidadAlumnos; }
}
