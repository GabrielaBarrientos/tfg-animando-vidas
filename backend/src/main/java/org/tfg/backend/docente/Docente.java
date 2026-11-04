package org.tfg.backend.docente;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Datos personales del docente, compartidos por todas sus participaciones. */
@Entity
@Table(name = "docente")
public class Docente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    private Long idDocente;

    @Column(name = "nombres", nullable = false, length = 200)
    private String nombres;
    @Column(name = "apellidos", nullable = false, length = 200)
    private String apellidos;
    @Column(name = "email", nullable = false, length = 320)
    private String email;
    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;
    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Docente() { }

    public Docente(DocenteRequest datos) { actualizarDatos(datos); }

    public void actualizarDatos(DocenteRequest datos) {
        nombres = datos.nombres().trim();
        apellidos = datos.apellidos().trim();
        email = datos.email().trim().toLowerCase(Locale.ROOT);
        telefono = datos.telefono().trim();
    }

    @PrePersist
    protected void antesDeCrear() {
        fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC);
        fechaActualizacion = fechaCreacion;
    }
    @PreUpdate
    protected void antesDeActualizar() { fechaActualizacion = OffsetDateTime.now(ZoneOffset.UTC); }

    public Long getIdDocente() { return idDocente; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
}
