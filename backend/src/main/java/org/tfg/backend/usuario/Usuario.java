
package org.tfg.backend.usuario;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "google_sub", length = 255, unique = true)
    private String googleSub;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @Column(name = "nombres", length = 200, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 200, nullable = false)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20, nullable = false)
    private RolUsuario rol;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado;

    @Column(name = "f_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "f_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Usuario() {
        // Constructor requerido por JPA
    }

    public Usuario(
            Long idUsuario,
            String googleSub,
            String email,
            String nombres,
            String apellidos,
            RolUsuario rol,
            boolean habilitado,
            OffsetDateTime fechaCreacion,
            OffsetDateTime fechaActualizacion) {

        this.idUsuario = idUsuario;
        this.googleSub = googleSub;
        this.email = email;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.rol = rol;
        this.habilitado = habilitado;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    @PrePersist
    protected void antesDeCrear() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }

        fechaActualizacion = ahora;
    }

    @PreUpdate
    protected void antesDeActualizar() {
        fechaActualizacion = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
