package org.tfg.backend.docente;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.tfg.backend.proyecto.Proyecto;

@Entity
@Table(name = "docente")
public class Docente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    private Long idDocente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, updatable = false)
    private Proyecto proyecto;

    @Column(name = "nombres", length = 200, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 200, nullable = false)
    private String apellidos;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @Column(name = "pais", length = 100, nullable = false)
    private String pais;

    @Column(name = "provincia", length = 150, nullable = false)
    private String provincia;

    @Column(name = "centro_educativo", length = 200, nullable = false)
    private String centroEducativo;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Docente() {
        // Constructor de JPA.
    }

    public Docente(Proyecto proyecto, DocenteRequest datos) {
        this.proyecto = proyecto;
        actualizarDatos(datos);
    }

    public void actualizarDatos(DocenteRequest datos) {
        nombres = datos.nombres().trim();
        apellidos = datos.apellidos().trim();
        email = datos.email().trim().toLowerCase(java.util.Locale.ROOT);
        pais = datos.pais().trim();
        provincia = datos.provincia().trim();
        centroEducativo = datos.centroEducativo().trim();
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

    public Long getIdDocente() { return idDocente; }
    public Proyecto getProyecto() { return proyecto; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getPais() { return pais; }
    public String getProvincia() { return provincia; }
    public String getCentroEducativo() { return centroEducativo; }
}
