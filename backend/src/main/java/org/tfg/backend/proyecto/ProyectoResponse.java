
package org.tfg.backend.proyecto;

public class ProyectoResponse {

    private Long idProyecto;
    private String nombre;
    private Short anio;
    private String descripcion;
    private Long idEmpresa;
    private String estado;
    private String nombreCompleto;

    public ProyectoResponse(Proyecto proyecto) {

        this.idProyecto = proyecto.getIdProyecto();
        this.nombre = proyecto.getNombre();
        this.anio = proyecto.getAnio();
        this.descripcion = proyecto.getDescripcion();
        this.idEmpresa = proyecto.getEmpresa().getIdEmpresa();
        this.estado = proyecto.getEstado().name();
        this.nombreCompleto = proyecto.getNombre()
        + " - " + proyecto.getEmpresa().getNombre()
        + " - " + proyecto.getAnio();
    }

    public Long getIdProyecto() {
        return idProyecto;
    }

    public String getNombre() {
        return nombre;
    }

    public Short getAnio() {
        return anio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public String getEstado() {
        return estado;
    }
    public String getNombreCompleto() {
    return nombreCompleto;
    }
}
